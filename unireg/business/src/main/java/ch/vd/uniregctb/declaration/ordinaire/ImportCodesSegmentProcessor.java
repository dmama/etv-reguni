package ch.vd.uniregctb.declaration.ordinaire;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class ImportCodesSegmentProcessor {

	private static final Logger LOGGER = Logger.getLogger(ImportCodesSegmentProcessor.class);

	private static final int BATCH_SIZE = 200;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;

	public ImportCodesSegmentProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
	}

	public ImportCodesSegmentResults run(List<ContribuableAvecCodeSegment> input, @Nullable StatusManager s) {
		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		final ImportCodesSegmentResults rapportFinal = new ImportCodesSegmentResults();
		final BatchTransactionTemplate<ContribuableAvecCodeSegment, ImportCodesSegmentResults> batchTemplate = new BatchTransactionTemplate<ContribuableAvecCodeSegment, ImportCodesSegmentResults>(input, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		batchTemplate.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<ContribuableAvecCodeSegment, ImportCodesSegmentResults>() {
			@Override
			public boolean doInTransaction(List<ContribuableAvecCodeSegment> batch, ImportCodesSegmentResults rapport) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0).getNoContribuable() + "; " + batch.get(batch.size() - 1).getNoContribuable() + "] ...", percent);
				doBatch(batch, rapport);
				return !status.interrupted();
			}

			@Override
			public ImportCodesSegmentResults createSubRapport() {
				return new ImportCodesSegmentResults();
			}
		});

		rapportFinal.setInterrompu(status.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void doBatch(List<ContribuableAvecCodeSegment> batch, ImportCodesSegmentResults rapport) {
		for (ContribuableAvecCodeSegment ctb : batch) {
			final Tiers tiers = tiersService.getTiers(ctb.getNoContribuable());
			if (tiers == null) {
				// contribuable inconnu...
				rapport.addErrorCtbInconnu(ctb.getNoContribuable());
			}
			else if (!(tiers instanceof Contribuable)) {
				// pas un contribuable -> pas de déclaration...
				rapport.addErrorPasUnContribuable(ctb.getNoContribuable(), tiers.getNatureTiers());
			}
			else {
				final Declaration derniereDeclaration = tiers.getDerniereDeclaration();
				if (derniereDeclaration == null) {
					// contribuable sans déclaration -> comment a-t-on fait pour assigner un code de segmentation (sensé être basé sur la taxation de l'année dernière...) ?
					rapport.addErrorCtbSansDeclaration(ctb.getNoContribuable());
				}
				else if (derniereDeclaration instanceof DeclarationImpotOrdinaire) {
						final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) derniereDeclaration;
						final Integer ancienCodeSegment = di.getCodeSegment();
						if (ancienCodeSegment == null || ancienCodeSegment != ctb.getCodeSegment()) {
							setNewCodeSegment(di, ctb.getCodeSegment());
							rapport.addCtbTraite(ctb.getNoContribuable(), ctb.getCodeSegment());
						}
						else {
							rapport.addCtbIgnoreDejaBonCode(ctb.getNoContribuable());
						}
				}
				else {
					// si ce ne sont pas des déclarations d'impôt ordinaires, que sont-ce ?
					rapport.addErrorCtbAvecMauvaisTypeDeDeclaration(ctb.getNoContribuable(), derniereDeclaration.getClass().getName());
				}
			}
		}
	}

	private void setNewCodeSegment(final DeclarationImpotOrdinaire di, final int codeSegment) {
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery query = session.createSQLQuery("UPDATE DECLARATION SET CODE_SEGMENT=:codeSegment, LOG_MDATE=:mdate, LOG_MUSER=:muser WHERE ID=:id");
				query.setInteger("codeSegment", codeSegment);
				query.setTimestamp("mdate", DateHelper.getCurrentDate());
				query.setString("muser", AuthenticationHelper.getCurrentPrincipal());
				query.setLong("id", di.getId());
				query.executeUpdate();
				return null;
			}
		});
	}
}
