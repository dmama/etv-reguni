package ch.vd.uniregctb.registrefoncier.dataimport.processor;

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
 * Contient les droits fermés et ouverts <i>pour un immeuble particulier</i> suite à un import des données RF à une date particulière.
 */
public class AffaireRF {

	@Nullable
	private final RegDate dateValeur;
	private final ImmeubleRF immeuble;
	private final List<DroitProprieteRF> ouverts;
	private final List<Pair<DroitProprieteRF, DroitProprieteRF>> miseajour;
	private final List<DroitProprieteRF> fermes;

	/**
	 * Crée une mutation des droit RF qui n'a pas encore été traitée et qui devra être appliquée sur les données de la DB.
	 *
	 * @param dateValeur la date de valeur (non-métier) de changements sur les droits
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
	 * Crée une mutation sur des droits à partir de données déjà sauvées en DB.
	 *
	 * @param dateValeur la date technique de changements sur les droits
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
	 * @return la date de l'import qui a provoqué la fermeture et l'ouverture des droits (nul pour l'import initial)
	 */
	@Nullable
	public RegDate getDateValeur() {
		return dateValeur;
	}

	/**
	 * Applique la mutation en DB : sauve les nouveaux droits, met-à-jour les droits existants et ferme les droits à fermer.
	 *
	 * @param audit un callback pour auditer les changements
	 */
	public void apply(@NotNull DroitRFDAO droitRFDAO, @Nullable AffaireRFAudit audit) {

		// on ajoute toutes les nouveaux droits
		ouverts.forEach(d -> processOuverture(d, droitRFDAO, audit));

		// on met-à-jour tous les droits qui changent (c'est-à-dire les changements dans les raisons d'acquisition)
		miseajour.forEach(p -> processMiseAJour(p.getFirst(), p.getSecond(), audit));

		// on ferme toutes les droits à fermer
		fermes.forEach(this::processFermeture);
	}

	/**
	 * Recalcule les dates de début <i>métier</i> des droits ouverts ou modifiés à la date de valeur des changements.
	 *
	 * @param audit un callback pour auditer les changements
	 */
	public void refreshDatesDebutMetier(@Nullable AffaireRFAudit audit) {
		ouverts.forEach(d -> refreshDateDebutMetier(d, audit));
		miseajour.forEach(pair -> refreshDateDebutMetier(pair.getSecond(), audit));
	}

	private void processOuverture(@NotNull DroitProprieteRF droit, @NotNull DroitRFDAO droitRFDAO, @Nullable AffaireRFAudit audit) {

		// on insère le droit en DB
		droit.setDateDebut(dateValeur);
		droit = (DroitProprieteRF) droitRFDAO.save(droit);

		// on calcule la date de début métier
		calculateDateEtMotifDebut(droit);

		// [SIFISC-24553] on met-à-jour à la main de la liste des servitudes pour pouvoir parcourir le graphe des dépendances dans le DatabaseChangeInterceptor
		immeuble.addDroitPropriete(droit);
		droit.getAyantDroit().addDroitPropriete(droit);

		// on publie l'événement fiscal correspondant
		if (audit != null) {
			audit.addCreated(droit);
		}
	}

	private void processFermeture(@NotNull DroitProprieteRF d) {
		if (dateValeur == null) {
			throw new IllegalArgumentException("La date de valeur est nulle : il s'agit donc de l'import initial et il ne devrait pas y avoir de droits à fermer");
		}
		d.setDateFin(dateValeur.getOneDayBefore());
	}

	private void processMiseAJour(@NotNull DroitProprieteRF droit, @NotNull DroitProprieteRF persisted, @Nullable AffaireRFAudit audit) {
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

		final RegDate dateDebutMetierPrecedent = persisted.getDateDebutMetier();
		final String motifDebutPrecedent = persisted.getMotifDebut();

		// on recalcule la date de début métier
		calculateDateEtMotifDebut(persisted);

		// on publie l'événement fiscal correspondant
		if (audit != null) {
			audit.addUpdated(persisted, dateDebutMetierPrecedent, motifDebutPrecedent);
		}
	}

	private void refreshDateDebutMetier(@NotNull DroitProprieteRF droit, @Nullable AffaireRFAudit audit) {
		final RegDate dateDebutPrecedente = droit.getDateDebutMetier();
		final String motifDebutPrecedent = droit.getMotifDebut();

		// on force le recalcul de la date de début du droit
		calculateDateEtMotifDebut(droit);

		if (audit != null) {
			final RegDate dateDebutCourant = droit.getDateDebutMetier();
			final String motifDebutCourant = droit.getMotifDebut();

			if (dateDebutCourant != dateDebutPrecedente || !Objects.equals(motifDebutCourant, motifDebutPrecedent)) {
				// le droit a été mis-à-jour
				audit.addUpdated(droit, dateDebutPrecedente, motifDebutPrecedent);
			}
			else {
				audit.addUntouched(droit);
			}
		}
	}

	void calculateDateEtMotifDebut(@NotNull DroitProprieteRF droit) {
		final Set<RaisonAcquisitionRF> raisonsAcquisition = droit.getRaisonsAcquisition();
		if (raisonsAcquisition == null || raisonsAcquisition.isEmpty()) {
			setDebutRaisonAcquisition(droit, null);
		}
		else {
			final DroitProprieteRF precedent = findDroitPrecedent(droit);
			if (precedent == null || precedent.getRaisonsAcquisition() == null) {
				// il n'y a pas de droit précédent : on prend la raison d'acquisition la plus vieille comme référence
				final RaisonAcquisitionRF first = raisonsAcquisition.stream()
						.filter(AnnulableHelper::nonAnnule)
						.min(Comparator.naturalOrder())
						.orElse(null);
				setDebutRaisonAcquisition(droit, first);
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
				setDebutRaisonAcquisition(droit, nouvelle);
			}
		}
	}

	private static void setDebutRaisonAcquisition(@NotNull DroitProprieteRF droit, @Nullable RaisonAcquisitionRF raison) {
		if (raison == null) {
			droit.setDateDebutMetier(null);
			droit.setMotifDebut(null);
		}
		else {
			droit.setDateDebutMetier(raison.getDateAcquisition());
			droit.setMotifDebut(raison.getMotifAcquisition());
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
