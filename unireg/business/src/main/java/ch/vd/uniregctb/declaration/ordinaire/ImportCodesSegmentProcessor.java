package ch.vd.uniregctb.declaration.ordinaire;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class ImportCodesSegmentProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportCodesSegmentProcessor.class);

	private static final int BATCH_SIZE = 200;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public ImportCodesSegmentProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public ImportCodesSegmentResults run(List<ContribuableAvecCodeSegment> input, @Nullable StatusManager s) {
		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		final ImportCodesSegmentResults rapportFinal = new ImportCodesSegmentResults(tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<ContribuableAvecCodeSegment, ImportCodesSegmentResults>
				batchTemplate = new BatchTransactionTemplateWithResults<>(input, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		batchTemplate.execute(rapportFinal, new BatchWithResultsCallback<ContribuableAvecCodeSegment, ImportCodesSegmentResults>() {
			@Override
			public boolean doInTransaction(List<ContribuableAvecCodeSegment> batch, ImportCodesSegmentResults rapport) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0).getNoContribuable() + "; " + batch.get(batch.size() - 1).getNoContribuable() + "] ...", progressMonitor.getProgressInPercent());
				doBatch(batch, rapport);
				return !status.interrupted();
			}

			@Override
			public ImportCodesSegmentResults createSubRapport() {
				return new ImportCodesSegmentResults(tiersService, adresseService);
			}
		}, progressMonitor);

		rapportFinal.setInterrompu(status.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	private void doBatch(List<ContribuableAvecCodeSegment> batch, ImportCodesSegmentResults rapport) {
		for (ContribuableAvecCodeSegment ctb : batch) {
			if (ctb.getCodeSegment() < 0 || ctb.getCodeSegment() > 9) {
				// un chiffre en base 10, et c'est tout (j'ai mis le test ici et pas dans l'import du fichier pour
				// que cette erreur soit remontée dans le rapport d'exécution, et pas seulement dans les logs techniques)
				rapport.addErrorCodeSegmentInvalide(ctb.getNoContribuable(), ctb.getCodeSegment());
			}
			else {
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
					else if (derniereDeclaration instanceof DeclarationImpotOrdinairePP) {
							final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) derniereDeclaration;
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
	}

	private void setNewCodeSegment(final DeclarationImpotOrdinairePP di, final int codeSegment) {
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
