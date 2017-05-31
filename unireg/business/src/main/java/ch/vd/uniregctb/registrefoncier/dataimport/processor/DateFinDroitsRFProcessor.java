package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.TraitementFinsDeDroitRFResults;

/**
 * [SIFISC-22997] Processeur qui détermine les dates de fin de droits sur les immeubles du registre foncier.
 */
public class DateFinDroitsRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateFinDroitsRFProcessor.class);

	private static final String MOTIF_ACHAT = "Achat";
	private static final String MOTIF_VENTE = "Vente";

	private final ImmeubleRFDAO immeubleRFDAO;
	private final PlatformTransactionManager transactionManager;

	public DateFinDroitsRFProcessor(@NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull PlatformTransactionManager transactionManager) {
		this.immeubleRFDAO = immeubleRFDAO;
		this.transactionManager = transactionManager;
	}

	public TraitementFinsDeDroitRFResults process(int nbThreads, @Nullable StatusManager s) {

		final StatusManager statusManager = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final TraitementFinsDeDroitRFResults finalResults = new TraitementFinsDeDroitRFResults(nbThreads);

		// on va chercher les ids des immeubles à processer
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = template.execute(status -> immeubleRFDAO.findImmeubleIdsAvecDatesDeFinDroitsACalculer());

		// on processe les immeubles
		final ParallelBatchTransactionTemplateWithResults<Long, TraitementFinsDeDroitRFResults> pt =
				new ParallelBatchTransactionTemplateWithResults<Long, TraitementFinsDeDroitRFResults>(ids,
				                                                                                      20,
				                                                                                      nbThreads,
				                                                                                      Behavior.REPRISE_AUTOMATIQUE,
				                                                                                      transactionManager,
				                                                                                      statusManager,
				                                                                                      AuthenticationInterface.INSTANCE) {
					@Override
					protected boolean isTransactionNeededOnAfterTransactionRollbackCall() {
						return false;
					}
				};
		pt.execute(finalResults, new BatchWithResultsCallback<Long, TraitementFinsDeDroitRFResults>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public TraitementFinsDeDroitRFResults createSubRapport() {
				return new TraitementFinsDeDroitRFResults(nbThreads);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, TraitementFinsDeDroitRFResults results) throws Exception {
				first.set(batch.get(0));
				statusManager.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				batch.forEach(id -> processImmeuble(id, results));
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.warn("Erreur pendant le traitement de l'immeuble id=[" + first.get() + "] : " + e.getMessage(), e);
				}
			}
		}, progressMonitor);

		finalResults.end();
		return finalResults;
	}

	void processImmeuble(Long id, TraitementFinsDeDroitRFResults results) {

		final ImmeubleRF immeuble = immeubleRFDAO.get(id);

		// on essaie d'abord de calculer les dates de fin en se basant sur les ayant-droits pour regrouper les droits.
		processImmeuble(immeuble, GroupingStrategy.AYANT_DROIT, results);

		// pour tous les droits sans date de fin métier, on se base sur la date technique de l'import (= regroupement de tous les droits ayant changé entre deux imports).
		processImmeuble(immeuble, GroupingStrategy.DATE_AFFAIRE, results);
	}

	private void processImmeuble(ImmeubleRF immeuble, GroupingStrategy groupingStrategy, TraitementFinsDeDroitRFResults results) {
		final List<DroitProprieteRF> droits = immeuble.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList());

		droits.sort(new DateRangeComparator<>());   // tri par ordre croissant chronologique des dates d'import

		// on regroupe les droits par affaire pour pouvoir lier les débuts de certains droits aux fins des autres
		final Map<GroupingKey, Affaire> affaires = new HashMap<>();
		for (DroitProprieteRF droit : droits) {
			if (droit.getDateFin() != null && droit.getDateFinMetier() == null) {
				// le droit a été fermé, on l'ajoute à la liste des anciens droits
				final RegDate dateAffaire = droit.getDateFin().getOneDayAfter();
				final Affaire affaire = affaires.computeIfAbsent(groupingStrategy.newKey(dateAffaire, droit), key -> new Affaire(immeuble.getId(), key.getDateAffaire()));
				affaire.addAncientDroit(droit);
			}
			if (droit.getDateDebut() != null) {
				// le droit a été ouvert, on l'ajoute à la liste des nouveaux droits
				final RegDate dateAffaire = droit.getDateDebut();
				final Affaire affaire = affaires.get(groupingStrategy.newKey(dateAffaire, droit));
				if (affaire != null) {  // l'affaire peut ne pas exister si tous les anciens droits avaient déjà une date de fin métier
					affaire.addNouveauDroit(droit);
				}
			}
		}

		// on processe chaque affaire
		affaires.values().forEach(a -> a.processAffaire(results));
	}

	/**
	 * Stratégie de regroupement des droits en affaires pour la détermination des dates de fin.
	 */
	private enum GroupingStrategy {
		/**
		 * Regroupement des droits basé sur le couple date d'import + id de l'ayant-droit (critère le plus précis). Utilisé principalement pour détecter les évolutions (changement de quote-part) du droit de propriété d'un propriétaire.
		 */
		AYANT_DROIT {
			@Override
			public GroupingKey newKey(@NotNull RegDate dateAffaire, DroitProprieteRF droit) {
				return new GroupingKey(dateAffaire, null, droit.getAyantDroit().getId());
			}
		},
		/**
		 * Regroupement des droits basé sur la date d'import (critère le plus large). Utilisé en dernier recours lorsque tous les autres modes de regroupement ont échoué.
		 */
		DATE_AFFAIRE {
			@Override
			public GroupingKey newKey(@NotNull RegDate dateAffaire, DroitProprieteRF droit) {
				return new GroupingKey(dateAffaire, null, null);
			}
		};

		/**
		 * Crée une clé de regroupement de droits basée sur les paramètres et le mode spécifiés.
		 *
		 * @param dateAffaire la date de l'affaire
		 * @param droit       le droit concerné
		 * @return une clé de regroupement.
		 */
		public abstract GroupingKey newKey(@NotNull RegDate dateAffaire, DroitProprieteRF droit);
	}

	/**
	 * Clé de regroupement des droits en affaires pour la détermination des dates de fin.
	 */
	private static class GroupingKey {
		@NotNull
		private final RegDate dateAffaire;
		@Nullable
		private final String masterId;
		@Nullable
		private final Long ayantDroitId;

		public GroupingKey(@NotNull RegDate dateAffaire, @Nullable String masterId, @Nullable Long ayantDroitId) {
			this.dateAffaire = dateAffaire;
			this.masterId = masterId;
			this.ayantDroitId = ayantDroitId;
		}

		@NotNull
		public RegDate getDateAffaire() {
			return dateAffaire;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final GroupingKey that = (GroupingKey) o;
			return Objects.equals(dateAffaire, that.dateAffaire) &&
					Objects.equals(masterId, that.masterId) &&
					Objects.equals(ayantDroitId, that.ayantDroitId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(dateAffaire, masterId, ayantDroitId);
		}
	}

	/**
	 * Représentation d'une affaire constituté de ventes et d'achats simultanés (ou considérés comme tel) pour un immeuble particulier.
	 */
	private static class Affaire {
		private final long immeubleId;
		private final RegDate dateAffaire;
		private final List<DroitRF> anciensDroits = new ArrayList<>();
		private final List<DroitRF> nouveauxDroits = new ArrayList<>();

		public Affaire(long immeubleId, RegDate dateAffaire) {
			this.immeubleId = immeubleId;
			this.dateAffaire = dateAffaire;
		}

		/**
		 * Processe l'affaire, c'est-à-dire détermine la date début <i>métier</i> de nouveaux droits (selon les règles fournies par Raphaël Carbo) et ferme les anciens droits à la même date.
		 *
		 * @param results les résultats du processeur à mettre-à-jour
		 */
		public void processAffaire(TraitementFinsDeDroitRFResults results) {

			// on détermine le nouveau droit qui va être utilisé pour fermer les anciens droits
			final DroitRF droitReference = nouveauxDroits.stream()
					.filter(d -> d.getDateDebutMetier() != null)
					.min(Comparator.comparing(DroitRF::getDateDebutMetier))     // on prend le droit avec la date la plus ancienne
					.orElse(null);

			if (droitReference != null) {
				// on ferme les anciens droits à la même date que la date de début du droit de référence (voir SIFISC-23525)
				fermeAnciensDroits(droitReference.getDateDebutMetier(), determineMotifFin(droitReference.getMotifDebut()));
				results.addImmeubleTraite(immeubleId);
			}
			else {
				results.addImmeubleIgnore(immeubleId, "Aucune date de début métier n'a été trouvée sur les nouveaux droits.");
			}
		}

		/**
		 * [SIFISC-23525] dito: "le motif de fin de l'ancien droit doit être égal au motif du nouveau droit, sauf pour le motif "achat" qui doit être traduit en "vente"."
		 */
		private static String determineMotifFin(String motifDebut) {
			return MOTIF_ACHAT.equals(motifDebut) ? MOTIF_VENTE : motifDebut;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public RegDate getDateAffaire() {
			return dateAffaire;
		}

		public void addAncientDroit(DroitRF d) {
			anciensDroits.add(d);
		}

		public void addNouveauDroit(DroitRF d) {
			nouveauxDroits.add(d);
		}

		private void fermeAnciensDroits(RegDate dateFinMetier, String motifFin) {
			anciensDroits.forEach(d -> {
				d.setDateFinMetier(dateFinMetier);
				d.setMotifFin(motifFin);
			});
		}
	}
}
