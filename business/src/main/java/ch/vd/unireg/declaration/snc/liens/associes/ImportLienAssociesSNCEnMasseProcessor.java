package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.LienAssociesEtSNC;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;

import static ch.vd.unireg.declaration.snc.liens.associes.DonneesLienAssocieEtSNC.DATE_DEBUT_LIEN;

/**
 * Processeur pour l'implémentation du job d'import en masse des liens entre tiers et la SNC.
 */
public class ImportLienAssociesSNCEnMasseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportLienAssociesSNCEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;

	public ImportLienAssociesSNCEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
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

					if (!(associe instanceof PersonnePhysique) && !(associe instanceof Entreprise)) {
						rapport.addContribuableNonAcceptable(associe, donneeBatch);
						continue;
					}

					//contrôle date de début
					final ForFiscalPrincipal dernierForSnc = snc.getDernierForFiscalPrincipal();
					final RegDate dateDebutDernierFor = dernierForSnc.getDateDebut();
					final RegDate regDateDebut = donneeBatch.getDateDebut();
					if (RegDateHelper.isAfter(dateDebutDernierFor, regDateDebut, NullDateBehavior.LATEST)) {
						LOGGER.info("La date du dernier For fiscale {} est superieur au " + DATE_DEBUT_LIEN, RegDateHelper.StringFormat.DISPLAY.toString(dateDebutDernierFor));
						donneeBatch.setDateDebut(dateDebutDernierFor);
					}

					if (dernierForSnc.getGenreImpot() != GenreImpot.REVENU_FORTUNE) {
						rapport.addContribuableNonAcceptable(snc, donneeBatch);
						continue;
					}

					if (tiersService.existLienEntreAssocieEtSNC(snc, associe, donneeBatch.getDateDebut())) {
						rapport.addDoublonNonAcceptable(donneeBatch);
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
