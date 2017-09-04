package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
	public AffaireRF(@NotNull RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull List<DroitProprieteRF> droits) {
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
		miseajour.forEach(p -> processMiseAJour(p.getFirst(), p.getSecond(), listener));

		// on ferme toutes les droits à fermer
		fermes.forEach(this::processFermeture);

		// on calcule tous les dates de début métier
		final List<Mutation> mutations = new ArrayList<>(ouverts.size() + miseajour.size());
		ouverts.stream()
				.map(d -> new Mutation(d, MutationType.CREATION))
				.forEach(mutations::add);
		miseajour.stream()
				.map(d -> new Mutation(d.getSecond(), MutationType.UPDATE))
				.forEach(mutations::add);
		calculateDatesDebutMetier(mutations, listener);
	}

	/**
	 * Recalcule les dates de début <i>métier</i> des droits ouverts ou modifiés à la date de valeur des changements.
	 *
	 * @param listener un callback pour écouter les changements
	 */
	public void refreshDatesDebutMetier(@Nullable AffaireRFListener listener) {

		// on recalcule tous les dates de début métier
		final List<Mutation> mutations = new ArrayList<>(ouverts.size() + miseajour.size());
		ouverts.stream()
				.map(d -> new Mutation(d, MutationType.UPDATE)) // update : car l'événement de création a déjà été envoyé
				.forEach(mutations::add);
		miseajour.stream()
				.map(d -> new Mutation(d.getSecond(), MutationType.UPDATE))
				.forEach(mutations::add);
		calculateDatesDebutMetier(mutations, listener);
	}

	/**
	 * Calcule ou recalcule les dates de début métier des droits spécifiés.
	 *
	 * @param mutations les droits et le context dans lequel ils sont traités
	 * @param listener     un callback pour écouter les changements
	 */
	private void calculateDatesDebutMetier(@NotNull List<Mutation> mutations, @Nullable AffaireRFListener listener) {

		// on calcule la date de début métier sur tous les droits
		mutations.forEach(m -> m.calculateDateEtMotifDebut(findDroitPrecedent(m.getDroit())));

		// on chercher la date de début métier la plus ancienne
		final RaisonAcquisitionRF raisonAcquisition = mutations.stream()
				.filter(m -> m.getDroit().getDateDebutMetier() != null || m.getDroit().getMotifDebut() != null)
				.min(Comparator.comparing(m -> m.getDroit().getDateDebutMetier()))
				.map(m -> new RaisonAcquisitionRF(m.getDroit().getDateDebutMetier(), m.getDroit().getMotifDebut(), null))
				.orElse(null);

		// [SIFISC-25583] on applique la date de début la plus ancienne sur tous les droits pour lesquels
		// la date de début métiet n'a pas pu être calculée de manière traditionnelle.
		mutations.stream()
				.filter(m -> m.getDroit().getDateDebutMetier() == null && m.getDroit().getMotifDebut() == null)
				.forEach(m -> m.setDebutRaisonAcquisition(raisonAcquisition));

		// on notifie le listener des changements si nécessaire
		if (listener != null) {
			mutations.forEach(m -> m.notifyAudit(listener));
		}
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

	private void processMiseAJour(@NotNull DroitProprieteRF droit, @NotNull DroitProprieteRF persisted, @Nullable AffaireRFListener listener) {
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

		// le droit a été mis-à-jour, on notifie le listener
		if (listener !=  null) {
			listener.addUpdated(persisted, persisted.getDateDebutMetier(), persisted.getMotifDebut());
		}
	}

	private enum MutationType {
		CREATION,
		UPDATE
	}

	private static class Mutation {
		private final DroitProprieteRF droit;
		private final MutationType type;

		private final RegDate dateDebutInitiale;
		private final String motifDebutInitial;

		public Mutation(@NotNull DroitProprieteRF droit, @NotNull MutationType type) {
			this.droit = droit;
			this.type = type;
			this.dateDebutInitiale = droit.getDateDebutMetier();
			this.motifDebutInitial = droit.getMotifDebut();
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

		public void notifyAudit(@NotNull AffaireRFListener listener) {

			final RegDate dateDebutCourant = droit.getDateDebutMetier();
			final String motifDebutCourant = droit.getMotifDebut();

			if ((dateDebutCourant != dateDebutInitiale || !Objects.equals(motifDebutCourant, motifDebutInitial))) {
				if (type == MutationType.CREATION) {
					listener.addCreated(droit);
				}
				else {
					listener.addUpdated(droit, dateDebutInitiale, motifDebutInitial);
				}
			}
			else {
				listener.addUntouched(droit);
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
}
