package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.RaisonAcquisitionRFHelper;

import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.masterIdAndVersionIdEquals;

/**
 * Représente les changements sur les droits pour <i>pour un immeuble particulier</i> à une date particulière.
 */
public class AffaireRF {

	@Nullable
	private final RegDate dateValeur;
	private final ImmeubleRF immeuble;
	private final List<DroitProprieteRF> ouverts;
	private final List<Pair<DroitProprieteRF, DroitProprieteRF>> miseajour;
	private final List<DroitProprieteRF> fermes;

	/**
	 * Crée une affaire qui n'a pas encore été traitée et dont les changements doivent être appliquée sur les données de la DB.
	 *
	 * @param dateValeur la date d'import en DB de l'affaire
	 * @param immeuble   l'immeuble concerné
	 * @param ouverts    les droits ouverts et à persister
	 * @param miseajour  les droits dont les raisons d'acquisition doivent être mises-à-jour (paire : nouveau droit -> droit déjà persisté)
	 * @param fermes     les droits déjà persistés à fermer
	 */
	public AffaireRF(@Nullable RegDate dateValeur,
	                 @NotNull ImmeubleRF immeuble,
	                 @NotNull List<DroitProprieteRF> ouverts,
	                 @NotNull List<Pair<DroitProprieteRF, DroitProprieteRF>> miseajour,
	                 @NotNull List<DroitProprieteRF> fermes) {
		this.dateValeur = dateValeur;
		this.immeuble = immeuble;
		this.ouverts = ouverts;
		this.fermes = fermes;
		this.miseajour = miseajour;
	}

	/**
	 * Crée une affaire dont les données ont déjà été sauvées en DB.
	 *
	 * @param dateValeur la date d'import en DB de l'affaire
	 * @param immeuble   l'immeuble concerné
	 * @param droits     les droits de l'immeuble (sans les droits annulés)
	 */
	public AffaireRF(@NotNull RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull Collection<DroitProprieteRF> droits) {
		final RegDate veilleImport = dateValeur.getOneDayBefore();
		this.dateValeur = dateValeur;
		this.immeuble = immeuble;
		this.ouverts = droits.stream()
				.filter(d -> d.getDateDebut() == dateValeur)
				.collect(Collectors.toList());
		this.miseajour = Collections.emptyList();
		this.fermes = droits.stream()
				.filter(d -> d.getDateFin() == veilleImport)
				.collect(Collectors.toList());
	}

	/**
	 * @return la date de l'affaire (nul si l'affaire a été traitée dans l'import initial)
	 */
	@Nullable
	public RegDate getDateValeur() {
		return dateValeur;
	}

	/**
	 * Applique le mutations en DB : sauve les nouveaux droits, met-à-jour les droits existants et ferme les droits à fermer.
	 *
	 * @param listener un callback pour écouter les changements
	 */
	public void apply(@NotNull DroitRFDAO droitRFDAO, @Nullable AffaireRFListener listener) {

		// on ajoute toutes les nouveaux droits
		final List<DroitProprieteRF> persisted = ouverts.stream()
				.map(d -> processOuverture(d, droitRFDAO))
				.collect(Collectors.toList());
		ouverts.clear();
		ouverts.addAll(persisted);

		// on met-à-jour tous les droits qui changent (c'est-à-dire les changements dans les raisons d'acquisition)
		miseajour.forEach(p -> processMiseAJour(p.getFirst(), p.getSecond()));

		// on ferme toutes les droits à fermer
		fermes.forEach(this::processFermeture);

		// on calcule tous les dates de début métier
		final List<Mutation> mutations = new ArrayList<>(fermes.size() + ouverts.size() + miseajour.size());
		fermes.stream()
				.map(d -> new Mutation(d, MutationType.CLOSING))
				.forEach(mutations::add);
		ouverts.stream()
				.map(d -> new Mutation(d, MutationType.CREATION))
				.forEach(mutations::add);
		miseajour.stream()
				.map(d -> new Mutation(d.getSecond(), MutationType.UPDATE))
				.forEach(mutations::add);
		calculateDatesMetier(mutations, Context.APPLY, listener);
	}

	/**
	 * Recalcule les dates de début/fin <i>métier</i> des droits ouverts ou modifiés à la date de valeur des changements.
	 *
	 * @param listener un callback pour écouter les changements
	 */
	public void refreshDatesMetier(@Nullable AffaireRFListener listener) {

		// on recalcule tous les dates de début métier
		final List<Mutation> mutations = new ArrayList<>(fermes.size() + ouverts.size() + miseajour.size());
		fermes.stream()
				.map(d -> new Mutation(d, MutationType.CLOSING))
				.forEach(mutations::add);
		ouverts.stream()
				.map(d -> new Mutation(d, MutationType.CREATION))
				.forEach(mutations::add);
		miseajour.stream()
				.map(d -> new Mutation(d.getSecond(), MutationType.UPDATE))
				.forEach(mutations::add);
		calculateDatesMetier(mutations, Context.REFRESH, listener);
	}

	/**
	 * Calcule ou recalcule les dates de début/fin métier des droits spécifiés.
	 *
	 * @param mutations les droits et le context dans lequel ils sont traités
	 * @param context   le context du calcul/recalcul des dates métier
	 * @param listener  un callback pour écouter les changements
	 */
	private void calculateDatesMetier(@NotNull List<Mutation> mutations, @NotNull Context context, @Nullable AffaireRFListener listener) {

		calculateDatesDebutMetier(mutations);
		calculateDatesFinMetier(mutations);

		// on notifie le listener des changements si nécessaire
		if (listener != null) {
			mutations.forEach(m -> m.notifyAudit(context, listener));
		}
	}

	/**
	 * Calcule ou recalcule les dates de début métier des droits spécifiés.
	 *
	 * @param mutations les droits et le context dans lequel ils sont traités
	 */
	private void calculateDatesDebutMetier(@NotNull List<Mutation> mutations) {

		// on ne recalcule pas les dates de début sur les droits fermés (car ils dépendent d'une autre affaire)
		final List<Mutation> filtered = mutations.stream()
				.filter(m -> m.getType() == MutationType.CREATION || m.getType() == MutationType.UPDATE)
				.collect(Collectors.toList());

		// on calcule la date de début métier sur tous les droits
		filtered.forEach(m -> m.calculateDateEtMotifDebut(findDroitPrecedent(m.getDroit())));

		// on chercher la date de début métier la plus ancienne
		final RaisonAcquisitionRF raisonAcquisition = filtered.stream()
				.filter(m -> m.getDroit().getDateDebutMetier() != null || m.getDroit().getMotifDebut() != null)
				.min(Comparator.comparing(m -> m.getDroit().getDateDebutMetier()))
				.map(m -> new RaisonAcquisitionRF(m.getDroit().getDateDebutMetier(), m.getDroit().getMotifDebut(), null))
				.orElse(null);

		// [SIFISC-25583] on applique la date de début la plus ancienne sur tous les droits pour lesquels
		// la date de début métiet n'a pas pu être calculée de manière traditionnelle.
		filtered.stream()
				.filter(m -> m.getDroit().getDateDebutMetier() == null && m.getDroit().getMotifDebut() == null)
				.forEach(m -> m.setDebutRaisonAcquisition(raisonAcquisition));
	}

	@NotNull
	private DroitProprieteRF processOuverture(@NotNull DroitProprieteRF droit, @NotNull DroitRFDAO droitRFDAO) {

		// on insère le droit en DB
		droit.setDateDebut(dateValeur);
		droit = (DroitProprieteRF) droitRFDAO.save(droit);

		// [SIFISC-24553] on met-à-jour à la main de la liste des servitudes pour pouvoir parcourir le graphe des dépendances dans le DatabaseChangeInterceptor
		immeuble.addDroitPropriete(droit);
		droit.getAyantDroit().addDroitPropriete(droit);

		return droit;
	}

	private void processFermeture(@NotNull DroitProprieteRF d) {
		if (dateValeur == null) {
			throw new IllegalArgumentException("La date de valeur est nulle : il s'agit donc de l'import initial et il ne devrait pas y avoir de droits à fermer");
		}
		d.setDateFin(dateValeur.getOneDayBefore());
	}

	private void processMiseAJour(@NotNull DroitProprieteRF droit, @NotNull DroitProprieteRF persisted) {
		if (!masterIdAndVersionIdEquals(droit, persisted)) {
			throw new IllegalArgumentException("Les mastersIdRF/versionIdRF sont différents : [" + droit.getMasterIdRF() + "/" + droit.getVersionIdRF() + "] " +
					                                   "et [" + persisted.getMasterIdRF() + "/" + persisted.getVersionIdRF() + "]");
		}
		if (!Objects.equals(droit.getPart(), persisted.getPart())) {
			throw new IllegalArgumentException("Les parts ne sont pas égales entre l'ancien et le nouveau avec le masterIdRF=[" + droit.getMasterIdRF() + "]");
		}

		final List<RaisonAcquisitionRF> toDelete = CollectionsUtils.newList(persisted.getRaisonsAcquisition());
		final List<RaisonAcquisitionRF> toAdd = CollectionsUtils.newList(droit.getRaisonsAcquisition());
		CollectionsUtils.removeCommonElements(toDelete, toAdd, RaisonAcquisitionRFHelper::dataEquals);   // on supprime les raison d'acquisition qui n'ont pas changé

		// on annule toutes les raisons en trop (on ne maintient pas d'historique dans les raisons d'acquisition car il s'agit déjà d'un historique)
		toDelete.forEach(r -> r.setAnnule(true));

		// on ajoute toutes les nouvelles raisons
		toAdd.forEach(persisted::addRaisonAcquisition);
	}

	private enum Context {
		APPLY,
		REFRESH
	}

	private enum MutationType {
		CREATION,
		UPDATE,
		CLOSING
	}

	private static class Mutation {
		private final DroitProprieteRF droit;
		private final MutationType type;

		private final RegDate dateDebutInitiale;
		private final String motifDebutInitial;
		private final RegDate dateFinInitiale;
		private final String motifFinInitial;

		public Mutation(@NotNull DroitProprieteRF droit, @NotNull MutationType type) {
			this.droit = droit;
			this.type = type;
			this.dateDebutInitiale = droit.getDateDebutMetier();
			this.motifDebutInitial = droit.getMotifDebut();
			this.dateFinInitiale = droit.getDateFinMetier();
			this.motifFinInitial = droit.getMotifFin();
		}

		public DroitProprieteRF getDroit() {
			return droit;
		}

		public MutationType getType() {
			return type;
		}

		public void calculateDateEtMotifDebut(@Nullable DroitProprieteRF precedent) {
			final Set<RaisonAcquisitionRF> raisonsAcquisition = droit.getRaisonsAcquisition();
			if (raisonsAcquisition == null || raisonsAcquisition.isEmpty()) {
				setDebutRaisonAcquisition(null);
			}
			else {
				if (precedent == null || precedent.getRaisonsAcquisition() == null) {
					// il n'y a pas de droit précédent : on prend la raison d'acquisition la plus vieille comme référence
					final RaisonAcquisitionRF first = raisonsAcquisition.stream()
							.filter(AnnulableHelper::nonAnnule)
							.min(Comparator.naturalOrder())
							.orElse(null);
					setDebutRaisonAcquisition(first);
				}
				else {
					// il y a bien un droit précédent : on prend la nouvelle raison d'acquisition comme référence
					final RegDate derniereDate = precedent.getRaisonsAcquisition().stream()
							.filter(AnnulableHelper::nonAnnule)
							.map(RaisonAcquisitionRF::getDateAcquisition)
							.max(Comparator.naturalOrder())
							.orElse(null);
					final RaisonAcquisitionRF nouvelle = raisonsAcquisition.stream()
							.filter(AnnulableHelper::nonAnnule)
							.filter(r -> RegDateHelper.isAfter(r.getDateAcquisition(), derniereDate, NullDateBehavior.EARLIEST))
							.min(Comparator.naturalOrder())
							.orElse(null);
					setDebutRaisonAcquisition(nouvelle);
				}
			}
		}

		public void setDebutRaisonAcquisition(@Nullable RaisonAcquisitionRF raison) {
			if (raison == null) {
				droit.setDateDebutMetier(null);
				droit.setMotifDebut(null);
			}
			else {
				droit.setDateDebutMetier(raison.getDateAcquisition());
				droit.setMotifDebut(raison.getMotifAcquisition());
			}
		}

		public void notifyAudit(@NotNull Context context, @NotNull AffaireRFListener listener) {

			final boolean debutChanged = droit.getDateDebutMetier() != dateDebutInitiale || !Objects.equals(droit.getMotifDebut(), motifDebutInitial);
			final boolean finChanged = droit.getDateFinMetier() != dateFinInitiale || !Objects.equals(droit.getMotifFin(), motifFinInitial);

			if (context == Context.REFRESH) {
				if (debutChanged || finChanged) {
					// dans le context de recalcul des dates métier, toutes les modifications sont des mises-à-jour (puisque les événements initiaux ont déjà été envoyés)
					if (debutChanged) {
						listener.onUpdateDateDebut(droit, dateDebutInitiale, motifDebutInitial);
					}
					if (finChanged) {
						listener.onUpdateDateFin(droit, dateFinInitiale, motifFinInitial);
					}
				}
			}
			else {
				// dans le context de l'application d'une affaire, on notifie simplement chaque mutation en fonction de son type.
				switch (type) {
				case CREATION:
					listener.onCreation(droit);
					break;
				case UPDATE:
					if (debutChanged || finChanged) {
						if (debutChanged) {
							listener.onUpdateDateDebut(droit, dateDebutInitiale, motifDebutInitial);
						}
						if (finChanged) {
							listener.onUpdateDateFin(droit, dateFinInitiale, motifFinInitial);
						}
					}
					else {
						// quelque chose d'autre à changé (par exemple : ajout d'une raison d'acquisition), on notifie quand même
						listener.onOtherUpdate(droit);
					}
					break;
				case CLOSING:
					listener.onClosing(droit);
					break;
				default:
					throw new IllegalArgumentException("Type de mutation inconnu = [" + type + "]");
				}
			}
		}
	}

	@Nullable
	private DroitProprieteRF findDroitPrecedent(@NotNull DroitProprieteRF courant) {

		// 1. on recherche le droit précédent par masterId (SIFISC-24987)
		DroitProprieteRF droitPrecedent = fermes.stream()
				.filter(d -> d.getMasterIdRF().equals(courant.getMasterIdRF()))
				.max(new DateRangeComparator<>())
				.orElse(null);
		if (droitPrecedent != null) {
			return droitPrecedent;
		}

		// 2. on recherche le droit précédent par propriétaire (SIFISC-25971)
		droitPrecedent = fermes.stream()
				.filter(d -> d.getAyantDroit().getId().equals(courant.getAyantDroit().getId()))
				.max(new DateRangeComparator<>())
				.orElse(null);
		if (droitPrecedent != null) {
			return droitPrecedent;
		}

		// pas trouvé
		return null;
	}

	/**
	 * [SIFISC-22997] Calcule ou recalcule les dates de fin métier des droits spécifiés.
	 *
	 * @param mutations les droits et le context dans lequel ils sont traités
	 */
	private void calculateDatesFinMetier(@NotNull List<Mutation> mutations) {

		// on essaie d'abord de calculer les dates de fin en se basant sur les ayant-droits pour regrouper les droits.
		calculateDatesFinMetier(mutations, GroupingStrategy.AYANT_DROIT);

		// pour tous les droits sans date de fin métier, on se base sur la date technique de l'import (= regroupement de tous les droits ayant changé entre deux imports).
		calculateDatesFinMetier(mutations, GroupingStrategy.DATE_TRANSACTION);
	}

	private void calculateDatesFinMetier(@NotNull List<Mutation> mutations, @NotNull GroupingStrategy groupingStrategy) {
		final List<DroitProprieteRF> droits = mutations.stream()
				// on ne recalcule pas les dates de fin sur les droits modifiés (car ils ne changent pas vraiment : seules leurs raisons d'acquisition changent)
				.filter(m -> m.getType() == MutationType.CREATION || m.getType() == MutationType.CLOSING)
				.map(Mutation::getDroit)
				.sorted(new DateRangeComparator<>()) // tri par ordre croissant chronologique des dates d'import
				.collect(Collectors.toList());

		// on regroupe les droits en transactions pour pouvoir lier les débuts de certains droits aux fins des autres
		final Map<GroupingKey, Transaction> transactions = new HashMap<>();
		for (DroitProprieteRF droit : droits) {
			if (droit.getDateFin() != null && droit.getDateFinMetier() == null) {
				// le droit a été fermé, on l'ajoute à la liste des anciens droits
				final RegDate dateTransaction = droit.getDateFin().getOneDayAfter();
				final Transaction transaction = transactions.computeIfAbsent(groupingStrategy.newKey(dateTransaction, droit), key -> new Transaction(key.getDateTransaction()));
				transaction.addAncientDroit(droit);
			}
			if (droit.getDateDebut() != null) {
				// le droit a été ouvert, on l'ajoute à la liste des nouveaux droits
				final RegDate dateTransaction = droit.getDateDebut();
				final Transaction transaction = transactions.get(groupingStrategy.newKey(dateTransaction, droit));
				if (transaction != null) {  // la transaction peut ne pas exister si tous les anciens droits avaient déjà une date de fin métier
					transaction.addNouveauDroit(droit);
				}
			}
		}

		// on processe chaque transaction
		transactions.values().forEach(Transaction::processTransaction);
	}

	/**
	 * Stratégie de regroupement des droits en transaction pour la détermination des dates de fin.
	 */
	private enum GroupingStrategy {
		/**
		 * Regroupement des droits basé sur le couple date d'import + id de l'ayant-droit (critère le plus précis). Utilisé principalement pour détecter les évolutions (changement de quote-part) du droit de propriété d'un propriétaire.
		 */
		AYANT_DROIT {
			@Override
			public GroupingKey newKey(@NotNull RegDate dateTransaction, DroitProprieteRF droit) {
				return new GroupingKey(dateTransaction, null, droit.getAyantDroit().getId());
			}
		},
		/**
		 * Regroupement des droits basé sur la date d'import (critère le plus large). Utilisé en dernier recours lorsque tous les autres modes de regroupement ont échoué.
		 */
		DATE_TRANSACTION {
			@Override
			public GroupingKey newKey(@NotNull RegDate dateTransaction, DroitProprieteRF droit) {
				return new GroupingKey(dateTransaction, null, null);
			}
		};

		/**
		 * Crée une clé de regroupement de droits basée sur les paramètres et le mode spécifiés.
		 *
		 * @param dateTransaction la date de la transaction
		 * @param droit           le droit concerné
		 * @return une clé de regroupement.
		 */
		public abstract GroupingKey newKey(@NotNull RegDate dateTransaction, DroitProprieteRF droit);
	}

	/**
	 * Clé de regroupement des droits en transactions pour la détermination des dates de fin.
	 */
	private static class GroupingKey {
		@NotNull
		private final RegDate dateTransaction;
		@Nullable
		private final String masterId;
		@Nullable
		private final Long ayantDroitId;

		public GroupingKey(@NotNull RegDate dateTransaction, @Nullable String masterId, @Nullable Long ayantDroitId) {
			this.dateTransaction = dateTransaction;
			this.masterId = masterId;
			this.ayantDroitId = ayantDroitId;
		}

		@NotNull
		public RegDate getDateTransaction() {
			return dateTransaction;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final GroupingKey that = (GroupingKey) o;
			return Objects.equals(dateTransaction, that.dateTransaction) &&
					Objects.equals(masterId, that.masterId) &&
					Objects.equals(ayantDroitId, that.ayantDroitId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(dateTransaction, masterId, ayantDroitId);
		}
	}

	/**
	 * Représentation d'une transaction constituté de ventes et d'achats simultanés (ou considérés comme tels) concernant un immeuble particulier.
	 */
	private static class Transaction {

		private static final String MOTIF_ACHAT = "Achat";
		private static final String MOTIF_VENTE = "Vente";

		private final RegDate dateTransaction;
		private final List<DroitRF> anciensDroits = new ArrayList<>();
		private final List<DroitRF> nouveauxDroits = new ArrayList<>();

		public Transaction(RegDate dateTransaction) {
			this.dateTransaction = dateTransaction;
		}

		/**
		 * Processe la transaction, c'est-à-dire recherche la date début <i>métier</i> de nouveaux droits (selon les règles fournies par Raphaël Carbo) et ferme les anciens droits à la même date.
		 */
		public void processTransaction() {

			// on détermine le nouveau droit qui va être utilisé pour fermer les anciens droits
			final DroitRF droitReference = nouveauxDroits.stream()
					.filter(d -> d.getDateDebutMetier() != null)
					.min(Comparator.comparing(DroitRF::getDateDebutMetier))     // on prend le droit avec la date la plus ancienne
					.orElse(null);

			if (droitReference != null) {
				// on ferme les anciens droits à la même date que la date de début du droit de référence (voir SIFISC-23525)
				fermeAnciensDroits(droitReference.getDateDebutMetier(), determineMotifFin(droitReference.getMotifDebut()));
			}
		}

		/**
		 * [SIFISC-23525] dito: "le motif de fin de l'ancien droit doit être égal au motif du nouveau droit, sauf pour le motif "achat" qui doit être traduit en "vente"."
		 */
		private static String determineMotifFin(String motifDebut) {
			return MOTIF_ACHAT.equals(motifDebut) ? MOTIF_VENTE : motifDebut;
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
	}}
