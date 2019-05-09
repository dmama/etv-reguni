package ch.vd.unireg.declaration.ordinaire.pp;

import javax.persistence.TemporalType;
import java.util.List;

import org.hibernate.query.NativeQuery;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

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
				return !status.isInterrupted();
			}

			@Override
			public ImportCodesSegmentResults createSubRapport() {
				return new ImportCodesSegmentResults(tiersService, adresseService);
			}
		}, progressMonitor);

		rapportFinal.setInterrompu(status.isInterrupted());
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
					final DeclarationImpotOrdinairePP derniereDI = tiers.getDerniereDeclaration(DeclarationImpotOrdinairePP.class);
					if (derniereDI == null) {
						final Declaration derniereDecla = tiers.getDerniereDeclaration(Declaration.class);
						if (derniereDecla != null) {
							// si ce ne sont pas des déclarations d'impôt ordinaires, que sont-ce ?
							rapport.addErrorCtbAvecMauvaisTypeDeDeclaration(ctb.getNoContribuable(), derniereDecla.getClass().getName());
						}
						else {
							// contribuable sans déclaration -> comment a-t-on fait pour assigner un code de segmentation (sensé être basé sur la taxation de l'année dernière...) ?
							rapport.addErrorCtbSansDeclaration(ctb.getNoContribuable());
						}
					}
					else {
						final Integer ancienCodeSegment = derniereDI.getCodeSegment();
						if (ancienCodeSegment == null || ancienCodeSegment != ctb.getCodeSegment()) {
							setNewCodeSegment(derniereDI, ctb.getCodeSegment());
							rapport.addCtbTraite(ctb.getNoContribuable(), ctb.getCodeSegment());
						}
						else {
							rapport.addCtbIgnoreDejaBonCode(ctb.getNoContribuable());
						}
					}
				}
			}
		}
	}

	private void setNewCodeSegment(final DeclarationImpotOrdinairePP di, final int codeSegment) {
		hibernateTemplate.execute(session -> {
			final NativeQuery query = session.createNativeQuery("UPDATE DOCUMENT_FISCAL SET CODE_SEGMENT=:codeSegment, LOG_MDATE=:mdate, LOG_MUSER=:muser WHERE ID=:id");
			query.setParameter("codeSegment", codeSegment);
			query.setParameter("mdate", DateHelper.getCurrentDate(), TemporalType.TIMESTAMP);
			query.setParameter("muser", AuthenticationHelper.getCurrentPrincipal());
			query.setParameter("id", di.getId());
			query.executeUpdate();
			return null;
		});
	}
}
