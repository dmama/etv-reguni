package ch.vd.uniregctb.tiers.rattrapage.ancienshabitants;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Processeur du traitement multi-threadé de récupération des noms/prénoms des parents des anciens habitants
 * depuis les données du registre civil
 */
public class RecuperationDonneesAnciensHabitantsProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RecuperationDonneesAnciensHabitantsProcessor.class);

	private static final int TAILLE_LOT = 10;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final ServiceCivilService serviceCivil;

	public RecuperationDonneesAnciensHabitantsProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersDAO tiersDAO, ServiceCivilService serviceCivil) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.serviceCivil = serviceCivil;
	}

	public RecuperationDonneesAnciensHabitantsResults run(final int nbThreads, final boolean forceEcrasement, final boolean parents, final boolean prenoms, StatusManager s) {

		final StatusManager statusManager = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final RecuperationDonneesAnciensHabitantsResults rapportFinal = new RecuperationDonneesAnciensHabitantsResults(nbThreads, forceEcrasement, parents, prenoms);

		// annonce de démarrage
		statusManager.setMessage("Recherche des anciens habitants concernés");
		final List<Long> ids = getIdsAnciensHabitants();
		if (ids != null && !ids.isEmpty()) {

			final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
			final ParallelBatchTransactionTemplateWithResults<Long, RecuperationDonneesAnciensHabitantsResults> template =
					new ParallelBatchTransactionTemplateWithResults<>(ids, TAILLE_LOT, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager,
					                                                  statusManager, AuthenticationInterface.INSTANCE);

			// c'est parti !
			template.execute(rapportFinal, new BatchWithResultsCallback<Long, RecuperationDonneesAnciensHabitantsResults>() {

				@Override
				public boolean doInTransaction(List<Long> batch, RecuperationDonneesAnciensHabitantsResults rapport) throws Exception {
					statusManager.setMessage(String.format("Traitement des %d anciens habitants trouvés", ids.size()), progressMonitor.getProgressInPercent());
					traiterBatch(batch, forceEcrasement, parents, prenoms, rapport);
					return true;
				}

				@Override
				public RecuperationDonneesAnciensHabitantsResults createSubRapport() {
					return new RecuperationDonneesAnciensHabitantsResults(nbThreads, forceEcrasement, parents, prenoms);
				}

			}, progressMonitor);
		}

		if (statusManager.interrupted()) {
			rapportFinal.setInterrupted(true);
			statusManager.setMessage("Le traitement a été interrompu.");
		}
		else {
			statusManager.setMessage("Traitement terminé.");
		}
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(List<Long> batch, boolean forceEcrasement, boolean parents, boolean prenoms, RecuperationDonneesAnciensHabitantsResults rapport) {
		for (Long id : batch) {
			final Tiers tiers = tiersDAO.get(id);
			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				if (pp.isHabitantVD()) {
					rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.HABITANT);
				}
				else if (pp.getNumeroIndividu() == null) {
					rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.JAMAIS_ETE_HABITANT);
				}
				else {
					// nous voilà donc en présence d'une personne physique ancienne habitante (= "ayant habité")
					final boolean dataMereDejaPresente = hasDataNomMere(pp);
					final boolean dataPereDejaPresent = hasDataNomPere(pp);
					final boolean dataPrenomsDejaPresents = hasDataPrenoms(pp);
					if ((!parents || (dataPereDejaPresent && dataMereDejaPresente)) && (!prenoms || dataPrenomsDejaPresents) && !forceEcrasement) {
						// pas besoin de faire quoi que ce soit -> tout est déjà là et on ne veut pas écraser
						rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE);
					}
					else {
						final Individu individu = serviceCivil.getIndividu(pp.getNumeroIndividu(), null);
						final NomPrenom nomOfficielMere = individu.getNomOfficielMere();
						final NomPrenom nomOfficielPere = individu.getNomOfficielPere();
						final String tousPrenoms = StringUtils.trimToNull(individu.getTousPrenoms());
						if (nomOfficielMere == null && nomOfficielPere == null && tousPrenoms == null) {
							// pas de données dans le civil -> rien ne peut être récupéré de toute façon
							rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.RIEN_DANS_CIVIL);
						}
						else if (!forceEcrasement && (!parents || (nomOfficielMere == null && dataPereDejaPresent) || (nomOfficielPere == null && dataMereDejaPresente)) && (!prenoms || dataPrenomsDejaPresents)) {
							// on a déjà la seule donnée présente, et on ne veut pas écraser...
							rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE);
						}
						else {
							final boolean majMere = parents && nomOfficielMere != null && (forceEcrasement || !dataMereDejaPresente);
							final boolean majPere = parents && nomOfficielPere != null && (forceEcrasement || !dataPereDejaPresent);
							final boolean majPrenoms = prenoms && tousPrenoms != null && (forceEcrasement || !dataPrenomsDejaPresents);

							// mise à jour des valeurs fiscales
							if (majMere) {
								pp.setPrenomsMere(nomOfficielMere.getPrenom());
								pp.setNomMere(nomOfficielMere.getNom());
							}
							if (majPere) {
								pp.setPrenomsPere(nomOfficielPere.getPrenom());
								pp.setNomPere(nomOfficielPere.getNom());
							}
							if (majPrenoms) {
								pp.setTousPrenoms(tousPrenoms);
							}

							if (!majPere && !majMere && !majPrenoms) {
								// on ne mets rien à jour...
								rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE);
							}
							else {
								rapport.addCasTraite(pp, majMere, majPere, majPrenoms);
							}
						}
					}
				}
			}
			else {
				rapport.addIgnore(id, RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.PAS_PERSONNE_PHYSIQUE);
			}
		}
	}

	private static boolean hasDataNomPere(PersonnePhysique pp) {
		return StringUtils.isNotBlank(pp.getNomPere()) || StringUtils.isNotBlank(pp.getPrenomsPere());
	}

	private static boolean hasDataNomMere(PersonnePhysique pp) {
		return StringUtils.isNotBlank(pp.getNomMere()) || StringUtils.isNotBlank(pp.getPrenomsMere());
	}

	private static boolean hasDataPrenoms(PersonnePhysique pp) {
		return StringUtils.isNotBlank(pp.getTousPrenoms());
	}

	private List<Long> getIdsAnciensHabitants() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final String hql = "select nh.id from PersonnePhysique nh where nh.habitant=false and nh.numeroIndividu is not null";
				return hibernateTemplate.find(hql, null);
			}
		});
	}
}
