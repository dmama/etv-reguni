package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.LienAssociesEtSNC;
import ch.vd.unireg.tiers.TiersService;

/**
 * Processeur pour l'implémentation du job d'import en masse des liens entre tiers et la SNC.
 */
public class ImportLienAssociesSNCEnMasseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportLienAssociesSNCEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final LienAssociesSNCService lienAssociesSNCService;

	public ImportLienAssociesSNCEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService,
	                                             LienAssociesSNCServiceImpl lienAssociesSNCService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.lienAssociesSNCService = lienAssociesSNCService;
	}

	public LienAssociesSNCEnMasseImporterResults run(final List<DonneesLienAssocieEtSNC> data, final RegDate dateTraitement, StatusManager s) {
		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);


		final LienAssociesSNCEnMasseImporterResults rapportFinal = new LienAssociesSNCEnMasseImporterResults(dateTraitement);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();

		final BatchTransactionTemplateWithResults<DonneesLienAssocieEtSNC, LienAssociesSNCEnMasseImporterResults>
				template = new BatchTransactionTemplateWithResults<>(data.iterator(), data.size(), BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<DonneesLienAssocieEtSNC, LienAssociesSNCEnMasseImporterResults>() {

			@Override
			public boolean doInTransaction(List<DonneesLienAssocieEtSNC> batch, LienAssociesSNCEnMasseImporterResults rapport) throws Exception {
				status.setMessage("Importation des liens entre associés et SNC", progressMonitor.getProgressInPercent());

				for (DonneesLienAssocieEtSNC donneeBatch : batch) {
					final Contribuable snc = hibernateTemplate.get(Contribuable.class, donneeBatch.getNoContribuableSNC());
					if (snc == null) {
						rapport.addContribuableInconnu(donneeBatch, "SNC ou SC inconnue d'Unireg");
						continue;
					}


					final Contribuable associe = hibernateTemplate.get(Contribuable.class, donneeBatch.getNoContribuableAssocie());
					if (associe == null) {
						rapport.addContribuableInconnu(donneeBatch, "Tiers associé inconnu d'Unireg");
						continue;
					}
					try {
						lienAssociesSNCService.isAllowed(associe, snc, donneeBatch.getDateDebut());
					}
					catch (LienAssociesEtSNCException e) {
						rapport.addContribuableNonAcceptable(snc, donneeBatch, e.getMessage());
						continue;
					}

					final LienAssociesEtSNC lienEntreAssociesEtSNC = new LienAssociesEtSNC(donneeBatch.getDateDebut(), null, associe, (Entreprise) snc);
					tiersService.addRapport(lienEntreAssociesEtSNC, associe, snc);
					rapport.addLienCree(donneeBatch);
					status.setMessage("Importation des liens entre associés et SNC", progressMonitor.getProgressInPercent());
				}

				return !status.isInterrupted();
			}

			@Override
			public LienAssociesSNCEnMasseImporterResults createSubRapport() {
				return new LienAssociesSNCEnMasseImporterResults(dateTraitement);
			}
		}, progressMonitor);

		status.setMessage("Traitement terminé.");

		// fin
		rapportFinal.setInterrupted(status.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}

}
