package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.Triplet;
import ch.vd.uniregctb.common.TripletIterator;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de base abstraite représentant une période d'assujettissement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class Assujettissement implements CollatableDateRange {

	private final Contribuable contribuable;
	private RegDate dateDebut;
	private RegDate dateFin;
	private final MotifFor motifFractDebut;
	private final MotifFor motifFractFin;
	protected final DecompositionFors fors;

	public Assujettissement(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut,
	                        MotifFor motifFractFin, DecompositionFors fors) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.contribuable = contribuable;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.motifFractDebut = motifFractDebut;
		this.motifFractFin = motifFractFin;
		this.fors = fors;
	}

	/**
	 * Permet de construire un assujettissement unique composé de deux assujettissement de même types qui se touchent
	 */
	protected Assujettissement(Assujettissement courant, Assujettissement suivant) {
		Assert.isTrue(courant.isCollatable(suivant));
		this.contribuable = courant.contribuable;
		this.dateDebut = courant.dateDebut;
		this.dateFin = suivant.dateFin;
		this.motifFractDebut = courant.motifFractDebut;
		this.motifFractFin = suivant.motifFractFin;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.fors = null; // ça n'a plus trop de sens
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	/**
	 * Expose le motif de début de l'assujettissement si celui-ci est fractionné, c'est-à-dire s'il ne débute pas le 1er janvier à cause :
	 * <ul>
	 * <li>d'un décès ou d'un veuvage,</li>
	 * <li>d'une arrivée ou d'un départ hors-Suisse, ou</li>
	 * <li>d'un passage sourcier/ordinaire ou vice-versa.</li>
	 * </ul>
	 * Dans tous les autres cas, la méthode retourne <b>null</b>.
	 * <p>
	 * <b>Note:</b> dans le cas limite où il y a bien un fractionnement de l'assujettissement, mais que celui-ci se trouve ajusté au début
	 * de mois (cas du passage source-ordinaire) et que le mois en question est janvier; on peut donc avoir un fractionnement de la période
	 * qui - après ajustement - tombe sur le 1er janvier. Dans ce cas-là, la méthode retourne malgré tout le motif de fractionnement
	 * initial.
	 *
	 * @return un motif de début de fractionnement ou <b>null</b> selon les règles exposées ci-dessus.
	 */
	public MotifFor getMotifFractDebut() {
		return motifFractDebut;
	}

	/**
	 * Expose le motif de fin de l'assujettissement si celui-ci est fractionné, c'est-à-dire s'il ne se termine pas le 31 décembre à cause :
	 * <ul>
	 * <li>d'un décès ou d'un veuvage,</li>
	 * <li>d'une arrivée ou d'un départ hors-Suisse, ou</li>
	 * <li>d'un passage sourcier/ordinaire ou vice-versa.</li>
	 * </ul>
	 * Dans tous les autres cas, la méthode retourne <b>null</b>.
	 * <p>
	 * <b>Note:</b> dans le cas limite où il y a bien un fractionnement de l'assujettissement, mais que celui-ci se trouve ajusté à la fin
	 * de mois (cas du passage source-ordinaire) et que le mois en question est décembre; on peut donc avoir un fractionnement de la période
	 * qui - après ajustement - tombe sur le 31 décembre. Dans ce cas-là, la méthode retourne malgré tout le motif de fractionnement
	 * initial.
	 *
	 * @return un motif de fin de fractionnement ou <b>null</b> selon les règles exposées ci-dessus.
	 */
	public MotifFor getMotifFractFin() {
		return motifFractFin;
	}

	public DecompositionFors getFors() {
		return fors;
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * @return une description orienté utilisateur du type d'assujettissement
	 */
	public abstract String getDescription();

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement durant l'année spécifiée. Dans la grande majorité des cas, il n'y a qu'une seule période d'assujettissement
	 * et elle coïncide avec l'année civile. Dans certains cas rares, il peut y avoir deux - voire même plus que de deux - périodes d'assujettissement distinctes.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param annee        l'année correspondant à la période fiscale considérée (du 1er janvier au 31 décembre)
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 */
	public static List<Assujettissement> determine(Contribuable contribuable, int annee) throws AssujettissementException {
		return determine(new DecompositionForsAnneeComplete(contribuable, annee));
	}

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement durant la période spécifiée.
	 * <p/>
	 * Cette méthode fonctionne en calculant l'assujettissement année après année et en collant les résultats l'un après l'autre. Elle n'est donc pas terriblement efficace, et dans la mesure du possible
	 * préférer la méthode {@link #determine(Contribuable, int)}.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param range        la période considérée
	 * @param collate      indique si on souhaite concaténer les assujettissement identique qui se suivent
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 */
	public static List<Assujettissement> determine(Contribuable contribuable, DateRange range, boolean collate)
			throws AssujettissementException {
		if (range != null && isFullYear(range)) {
			return determine(new DecompositionForsAnneeComplete(contribuable, range.getDateDebut().year()));
		}
		else {
			int anneeDebut;
			int anneeFin;

			if (range == null) {
				RegDate debut = contribuable.getDateDebutActivite();
				if (debut == null) {
					// aucun date de début d'activité, le contribuable ne possède pas de for et ne peut donc pas être assujetti
					return null;
				}
				anneeDebut = debut.year();

				RegDate fin = contribuable.getDateFinActivite();
				if (fin == null) {
					anneeFin = RegDate.get().year(); // annee courante
				}
				else {
					anneeFin = fin.year();
				}

			}
			else {
				anneeDebut = range.getDateDebut().year();
				anneeFin = range.getDateFin().year();
			}

			// Détermination des assujettissements sur toutes les années considérées
			List<Assujettissement> list = new ArrayList<Assujettissement>();
			for (int annee = anneeDebut; annee <= anneeFin; annee++) {
				List<Assujettissement> l = determine(contribuable, annee);
				if (l != null) {
					list.addAll(l);
				}
			}

			// Réduction des assujettissements de même type qui se touchent
			if (collate) {
				list = DateRangeHelper.collate(list);
			}

			// Réduction au range spécifié
			if (range != null) {
				List<Assujettissement> results = new ArrayList<Assujettissement>();
				for (Assujettissement a : list) {
					if (DateRangeHelper.intersect(a, range)) {
						results.add(a);
					}
				}
				list = results;
			}

			return list;
		}
	}

	private static boolean isFullYear(DateRange range) {
		final RegDate debut = range.getDateDebut();
		final RegDate fin = range.getDateFin();
		return debut.year() == fin.year() && debut.month() == 1 && debut.day() == 1 && fin.month() == 12 && fin.day() == 31;
	}

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement durant l'année spécifiée. Dans la grande majorité des cas, il n'y a qu'une seule période d'assujettissement
	 * et elle coïncide avec l'année civile. Dans certains cas rares, il peut y avoir deux - voire même plus que de deux - périodes d'assujettissement distinctes.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param annee        l'année correspondant à la période fiscale considérée (du 1er janvier au 31 décembre)
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 */
	public static List<Assujettissement> determine(DecompositionForsAnneeComplete fors) throws AssujettissementException {

		if (fors.isFullyEmpty()) {
			return null;
		}

		List<Assujettissement> assujettissements = new ArrayList<Assujettissement>();

		// Détermine les éventuelles sous-périodes d'imposition dues à des départs/arrivées hors-Suisse (et autres cas exotiques)
		final List<SousPeriode> sousPeriodes = extractSousPeriodes(fors);
		if (sousPeriodes.size() > 1) {

			// le fractionnement des périodes d'imposition est nécessaire
			for (SousPeriode p : sousPeriodes) {
				DecompositionForsPeriode f = new DecompositionForsPeriode(fors.contribuable, p);
				if (f.isEmpty()) {
					continue;
				}
				Assujettissement a = determinePeriode(f, p.getMotifFractDebut(), p.getMotifFractFin());
				if (a != null) {
					assujettissements.add(a);
				}
			}
		}
		else {

			// pas de fractionnement
			Assujettissement a = determinePeriode(fors, null, null);
			if (a != null) {
				assujettissements.add(a);
			}
		}

		adapteDatesDebutEtFin(assujettissements);

		return assujettissements.isEmpty() ? null : assujettissements;
	}

	/**
	 * Code de retour des méthodes d'adaption, utilisé à la place de booléens par mesure de clarté.
	 */
	private enum AdaptionResult {
		LISTE_NON_MODIFIEE,
		LISTE_MODIFIEE
	}

	/**
	 * Cette méthode adapte les dates de début et de fin de validité des assujettissements pour qu'elles tombent exactement sur des débuts et fins de mois; et ceci dans certains cas précis.
	 * <p/>
	 * Les dates de début/fin d'un assujettissement sont adaptées si les conditions suivantes sont respectées:
	 * <ul>
	 * <li>l'assujettissement est de type source pure</li>
	 * <li>l'assujettissement précédent/suivant est de type ordinaire non-mixte (ordinaire, dépense, ... mais pas sourcier mixte)</li>
	 * </ul>
	 * <p/>
	 * En gros, à chaque passage source pure à ordinaire et vice-versa les dates de début et fin sont adaptées.
	 */
	private static void adapteDatesDebutEtFin(List<Assujettissement> assujettissements) {
		final int size = assujettissements.size();
		if (size == 0) {
			return;
		}

		// Note: algo un peu spécial parce que le liste sur lequel est basé l'itérateur peut se trouver modifiée. Dans ce cas, on reprend le
		// travail au début (jusqu'à ce que toutes les modifications nécessaires aient été apportées).
		AdaptionResult res;
		do {
			res = AdaptionResult.LISTE_NON_MODIFIEE;
			final TripletIterator<Assujettissement> iter = new TripletIterator<Assujettissement>(assujettissements.iterator());

			while (res == AdaptionResult.LISTE_NON_MODIFIEE && iter.hasNext()) {
				final Triplet<Assujettissement> triplet = iter.next();
				res = adapteTriplet(assujettissements, triplet);
			}
		} while (res == AdaptionResult.LISTE_MODIFIEE);
	}

	/**
	 * Adapte les dates de fin et de début pour le triplet d'assujettissement courant.
	 *
	 * @param assujettissements la liste complète des assujettissements
	 * @param triplet           le triple courant
	 * @return {@link AdaptionResult#LISTE_NON_MODIFIEE} si les assujettissements précédent et suivant ont pu être adaptés sans problème; ou {@link AdaptionResult#LISTE_MODIFIEE} si l'un d'entre eux a
	 *         été supprimé de la liste et que cette dernière a donc été modifiée.
	 */
	private static AdaptionResult adapteTriplet(List<Assujettissement> assujettissements, final Triplet<Assujettissement> triplet) {

		AdaptionResult res = AdaptionResult.LISTE_NON_MODIFIEE;

		final Assujettissement courant = triplet.current;

		if (courant instanceof SourcierPur) {
			final RegDate debut = courant.getDateDebut();
			final RegDate fin = courant.getDateFin();

			// faut-il adapter la date de début ?
			final Assujettissement precedent = triplet.previous;
			if (precedent == null || !(precedent instanceof Sourcier)) {
				// on doit arrondir au début du mois
				final RegDate newDebut = RegDate.get(debut.year(), debut.month(), 1);
				if (newDebut != debut) {
					if (precedent != null && precedent.getDateFin().getOneDayAfter() == debut) {
						final AdaptionResult r = adapteDateFin(precedent, newDebut.getOneDayBefore(), assujettissements);
						if (r == AdaptionResult.LISTE_MODIFIEE) {
							res = AdaptionResult.LISTE_MODIFIEE;
						}
					}
					courant.setDateDebut(newDebut);
				}
			}

			// faut-il adapter la date de fin ?
			final Assujettissement suivant = triplet.next;
			if (suivant == null || !(suivant instanceof Sourcier)) {
				// on doit arrondir à la fin du mois
				final RegDate newFin = RegDate.get(fin.year(), fin.month(), 1).addMonths(1).getOneDayBefore();
				if (newFin != fin) {
					if (suivant != null && suivant.getDateDebut().getOneDayBefore() == fin) {
						final AdaptionResult r = adapteDateDebut(suivant, newFin.getOneDayAfter(), assujettissements);
						if (r == AdaptionResult.LISTE_MODIFIEE) {
							res = AdaptionResult.LISTE_MODIFIEE;
						}
					}
					courant.setDateFin(newFin);
				}
			}
		}

		return res;
	}

	/**
	 * Adapte la date de fin de l'assujettissement précédent à la nouvelle valeur spécifiée.
	 * <p/>
	 * Cette méthode applique intelligemment la nouvelle date, c'est-à-dire qu'elle s'assure que l'assujettissement précédent reste valide avec la nouvelle date. Si ce n'est pas le cas,
	 * l'assujettissement précédent est supprimé de la liste des assujettissements.
	 *
	 * @param precedent         l'assujettissement précédent dont la date de fin doit être adaptée.
	 * @param newDateFin        la nouvelle date de fin à appliquer.
	 * @param assujettissements la liste complète des assujettissements (qui doit contenir l'assujettissement précédent).
	 * @return {@link AdaptionResult#LISTE_NON_MODIFIEE} si l'assujettissement précédent à pu être adapté sans problème; ou {@link AdaptionResult#LISTE_MODIFIEE} si l'assujettissement a été supprimé de
	 *         la liste et que cette dernière a donc été modifiée.
	 */
	private static AdaptionResult adapteDateFin(Assujettissement precedent, RegDate newDateFin, List<Assujettissement> assujettissements) {
		if (precedent.getDateDebut().isBeforeOrEqual(newDateFin)) {
			precedent.setDateFin(newDateFin);
			return AdaptionResult.LISTE_NON_MODIFIEE;
		}
		else {
			// la nouvelle date de fin est *avant* la date de début de l'assujettissement précédent -> on l'annule
			final int index = assujettissements.indexOf(precedent);
			assujettissements.remove(index);

			// et il faut remonter dans la liste d'assujettissement pour adapter le précédent du précédent
			if (index > 0) {
				Assujettissement precedentprecedent = assujettissements.get(index - 1);
				adapteDateFin(precedentprecedent, newDateFin, assujettissements);
			}

			return AdaptionResult.LISTE_MODIFIEE;
		}
	}

	/**
	 * Adapte la date de début de l'assujettissement suivant à la nouvelle valeur spécifiée.
	 * <p/>
	 * Cette méthode applique intelligemment la nouvelle date, c'est-à-dire qu'elle s'assure que l'assujettissement suivant reste valide avec la nouvelle date. Si ce n'est pas le cas, l'assujettissement
	 * suivant est supprimé de la liste des assujettissements.
	 *
	 * @param suivant           l'assujettissement suivant dont la date de début doit être adaptée.
	 * @param newDateDebut      la nouvelle date de début à appliquer.
	 * @param assujettissements la liste complète des assujettissements (qui doit contenir l'assujettissement suivant).
	 * @return {@link AdaptionResult#LISTE_NON_MODIFIEE} si l'assujettissement suivant à pu être adapté sans problème; ou {@link AdaptionResult#LISTE_MODIFIEE} si l'assujettissement a été supprimé de la
	 *         liste et que cette dernière a donc été modifiée.
	 */
	private static AdaptionResult adapteDateDebut(Assujettissement suivant, RegDate newDateDebut, List<Assujettissement> assujettissements) {
		if (newDateDebut.isBeforeOrEqual(suivant.getDateFin())) {
			suivant.setDateDebut(newDateDebut);
			return AdaptionResult.LISTE_NON_MODIFIEE;
		}
		else {
			// la nouvelle date de début est *après* la date de fin de l'assujettissement suivant -> on l'annule
			final int index = assujettissements.indexOf(suivant);
			assujettissements.remove(index);

			// et il faut descendre dans la liste d'assujettissement pour adapter le suivant du suivant
			if (index < assujettissements.size()) {
				Assujettissement suivantsuivant = assujettissements.get(index);
				adapteDateDebut(suivantsuivant, newDateDebut, assujettissements);
			}

			return AdaptionResult.LISTE_MODIFIEE;
		}
	}

	private void setDateDebut(RegDate date) {
		this.dateDebut = date;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	private void setDateFin(RegDate date) {
		this.dateFin = date;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	/**
	 * Détecte le <b>potentiel</b> fractionnement d'une période fiscal complète (= une année) en sous-périodes d'assujettissement. Le fractionnement d'une période fiscale complète peut être nécessaire en
	 * présence de certains motifs d'ouvertures/fermetures de fors principaux (départ/arrivée hors-Suisse, par exemple).
	 *
	 * @param fors la décomposition des fors pour la période fiscale complète considérée
	 * @return une liste des sous-périodes <b>potentielles</b>, ou une liste vide si aucun fractionnement n'est nécessaire
	 */
	protected static List<SousPeriode> extractSousPeriodes(DecompositionForsAnneeComplete fors) {

		final RegDate dateDebutAnnee = fors.getDateDebut();
		final RegDate dateFinAnnee = fors.getDateFin();

		List<SousPeriode> ranges = new ArrayList<SousPeriode>();
		RegDate pivot = dateDebutAnnee;
		MotifFor motifPivot = null;

		final EvenementForsIterator iter = new EvenementForsIterator(fors);
		while (iter.hasNext()) {
			final EvenementFors event = iter.next();
			final MotifFor motifOuverture = fractionnementOuverture(event);
			if (motifOuverture != null) {
				final RegDate debut = event.dateEvenement;
				if (debut != null && debut.isAfter(pivot)) {
					// fractionne le range précédent
					ranges.add(new SousPeriode(pivot, debut.getOneDayBefore(), motifPivot, motifOuverture));
					pivot = debut;
					motifPivot = motifOuverture;
				}
			}
			final MotifFor motifFermeture = fractionnementFermeture(event);
			if (motifFermeture != null) {
				final RegDate fin = event.dateEvenement;
				if (fin != null && fin.isBefore(dateFinAnnee)) {
					// fractionne le range courant
					ranges.add(new SousPeriode(pivot, fin, motifPivot, motifFermeture));
					pivot = fin.getOneDayAfter();
					motifPivot = motifFermeture;
				}
			}
		}

		if (pivot != dateDebutAnnee && pivot.isBeforeOrEqual(dateFinAnnee)) {
			// ajoute le dernier range
			ranges.add(new SousPeriode(pivot, dateFinAnnee, motifPivot, null));
		}

		return ranges;
	}

	/**
	 * @param event   l'événement courant sur lequel il faut travailler
	 * @return <b>vrai</b> si le motif d'ouverture du for principal spécifié peut provoquer une fractionnement de l'assujettissement
	 */
	private static MotifFor fractionnementOuverture(EvenementFors event) {
		// voir la spécification "Envoyer automatiquement les DI-PP", §3.1.2 "Enregistrement des déclarations d'impôt"

		if (event.ouverts.principal != null) { // événement d'ouverture de for principal

			final MotifFor motif = event.ouverts.principal.getMotifOuverture();

			// Décès -> fractionnement dans tous les cas
			if (motif == MotifFor.VEUVAGE_DECES) {
				return motif;
			}

			// Arrivée hors-Suisse -> fractionnement pour autant que le contribuable arrive dans le canton de Vaud
			if (motif == MotifFor.ARRIVEE_HS && event.ouverts.principal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return motif;
			}

			// Le passage du rôle source pur au rôle ordinaire non-mixte (et vice versa) doit provoquer un fractionnement.
			if (event.actifsVeille.principal != null) {
				if ((roleSourcierPur(event.actifsVeille.principal) && roleOrdinaireNonMixte(event.ouverts.principal))
						|| (roleOrdinaireNonMixte(event.actifsVeille.principal) && roleSourcierPur(event.ouverts.principal))) {
					return MotifFor.CHGT_MODE_IMPOSITION;
				}
			}

			// [UNIREG-1742] Le départ ou l'arrivée hors-Canton d'un sourcier pur ou mixte 137 al. 2 (donc sans for secondaire) doit provoquer un fractionnement
			final ModeImposition modeImposition = event.ouverts.principal.getModeImposition();
			if ((modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2) && (motif == MotifFor.DEPART_HC || motif == MotifFor.ARRIVEE_HC)) {
				return motif;
			}
		}

		if (event.ouverts.secondaires != null) { // événement d'ouverture d'un for secondaire

			// [UNIREG-1742] ouverture d'un (ou plusieurs en même temps) for secondaire d'un contribuable hors-Suisse alors qu'il n'en existait pas avant -> début de rattachement économique
			if (event.actifs.principal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && (event.actifsVeille.principal == null || event.actifsVeille.secondaires == null)) {
				// détermine le motif le plus fort
				MotifFor motif = null;
				for (ForFiscalSecondaire fs : event.ouverts.secondaires) {
					if (motif == null || motif == MotifFor.ACHAT_IMMOBILIER) {
						Assert.notNull(fs.getMotifOuverture());
						motif = fs.getMotifOuverture();
					}
				}
				return motif;
			}
		}

		return null;
	}

	/**
	 * @param event l'événement courant sur lequel il faut travailler
	 * @return <b>vrai</b> si le motif d'ouverture du for principal spécifié peut provoquer une fractionnement de l'assujettissement
	 */
	private static MotifFor fractionnementFermeture(EvenementFors event) {

		if (event.fermes.principal != null) { // événement de fermeture du for principal
			final MotifFor motif = event.fermes.principal.getMotifFermeture();

			// Décès -> fractionnement dans tous les cas
			if (motif == MotifFor.VEUVAGE_DECES) {
				return motif;
			}

			// Départ hors-Suisse -> fractionnement pour autant que le contribuable parte du canton de Vaud
			if (motif == MotifFor.DEPART_HS && event.fermes.principal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return motif;
			}
			
			/*
			 * Note: on ne teste pas le passage source <-> ordinaire ici parce qu'il faudrait connaître le for principal suivant. Or ce for
			 * principal suivant sera testé à son tour dans la méthode #fractionnementOuverture(). Donc, d'un point de vue algorithmique le test
			 * n'est pas nécessaire ici.
			 */

			// [UNIREG-1742]  Le départ ou l'arrivée hors-Canton d'un sourcier pur ou mixte 137 al. 2 (donc sans for secondaire) doit provoquer un fractionnement
			final ModeImposition modeImposition = event.fermes.principal.getModeImposition();
			if ((modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2) && (motif == MotifFor.DEPART_HC || motif == MotifFor.ARRIVEE_HC)) {
				return motif;
			}
		}

		if (event.fermes.secondaires != null) { // événement de fermeture d'un for secondaire

			// [UNIREG-1742] fermeture du dernier (ou des derniers en même temps) for secondaire d'un contribuable hors-Suisse -> fin de rattachement économique
			if (event.actifs.principal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && (event.actifsLendemain.principal == null || event.actifsLendemain.secondaires == null)) {
				// détermine le motif le plus fort
				MotifFor motif = null;
				for (ForFiscalSecondaire fs : event.fermes.secondaires) {
					if (motif == null || motif == MotifFor.ACHAT_IMMOBILIER) {
						Assert.notNull(fs.getMotifFermeture());
						motif = fs.getMotifFermeture();
					}
				}
				return motif;
			}
		}

		return null;
	}

	private static boolean roleSourcierPur(ForFiscalPrincipal forPrecedent) {
		return ModeImposition.SOURCE.equals(forPrecedent.getModeImposition());
	}

	private static boolean roleOrdinaireNonMixte(ForFiscalPrincipal forCourant) {
		final ModeImposition mode = forCourant.getModeImposition();
		return !ModeImposition.SOURCE.equals(mode) && !ModeImposition.MIXTE_137_1.equals(mode) && !ModeImposition.MIXTE_137_2.equals(mode);
	}

	/**
	 * Retourne le type d'autorité fiscale du dernier for principal valide, ou <b>null</b> si aucune assujettissement n'est nécessaire.
	 *
	 * Il y a plusieurs cas valides pour qu'un contribuable ne possède pas de for principal en fin d'année :
	 * <ul>
	 * <li><b>qu'il soit décédé dans l'année</b> : la période d'imposition doit avoir été fractionnée et on ne devrait pas arriver ici.</li>
	 * <li><b>qu'il ait déménagé hors-Suisse dans l'année</b> : la période d'imposition doit avoir été fractionnée et on ne devrait pas
	 * arriver ici.</li>
	 * <li><b>qu'il se soit marié dans l'année</b> : le contribuable déterminant est le ménage commun et on peut ignorer la personne
	 * physique. Et vice-versa pour la séparation.</li>
	 * <li><b>qu'il ait déménagé hors-canton</b> : s'il y a des fors secondaires, on continue le traitement. Autrement, on peut ignorer le
	 * contribuable.</li>
	 * <li><b>qu'il habite hors-Suisse et que son for principal soit fermé sans motif</b> : s'il y a des fors secondaires, on continue le
	 * traitement. Autrement, on peut ignorer le contribuable [UNIREG-1390].</li>
	 * </ul>
	 */
	@SuppressWarnings("deprecation")
	private static TypeAutoriteFiscale determineTypeAutoriteFiscale(final DecompositionFors fors) throws AssujettissementException {

		final TypeAutoriteFiscale typeAutoriteFiscale;

		if (fors.principal == null) {
			final ForFiscalPrincipal dernier = fors.principauxDansLaPeriode.last();
			if (dernier == null) {
				final String message = String.format("Le contribuable n°%d ne possède aucun for principal dans la période %s.",
						fors.contribuable.getNumero(), DateRangeHelper.toString(fors));
				throw new AssujettissementException(message);
			}

			final MotifFor dernierMotif = dernier.getMotifFermeture();
			if (MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(dernierMotif)
					|| MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT.equals(dernierMotif)) {
				typeAutoriteFiscale = null;
			}
			else {
				/*
				 * La migration génère beaucoup de contribuables avec des fors fiscaux fermés avec un motif indéterminé. Ce qui pose un réel
				 * problème, puisque l'assujettissement du contribuable peut varier du tout-au-tout en fonction d'un départ HC (= pas
				 * d'assujettissement) ou d'un départ HS (= assujettissement partiel). La politique actuelle est de considérer le motif de
				 * départ INDETERMINE comme égale au motif DEPART_HC.
				 */
				final boolean departHC = MotifFor.DEPART_HC.equals(dernierMotif) || MotifFor.INDETERMINE.equals(dernierMotif); // départ hors canton
				final boolean forHC = TypeAutoriteFiscale.COMMUNE_HC.equals(dernier.getTypeAutoriteFiscale()); // for hors canton après départ
				final boolean forHS = TypeAutoriteFiscale.PAYS_HS.equals(dernier.getTypeAutoriteFiscale()); // for hors Suisse
				if (!departHC && !forHC && !forHS) {
					// motifs valides : voir javadoc de la méthode
					final String message = String.format("Le contribuable n°%d ne possède pas de for principal à la fin de la période %s"
							+ " et le motif de fermeture %s n'est pas valide dans ce cas-là.", fors.contribuable.getNumero(),
							DateRangeHelper.toString(fors), dernierMotif);
					throw new AssujettissementException(message);
				}

				if (fors.secondairesDansLaPeriode.isEmpty()) {
					typeAutoriteFiscale = null;
				}
				else {
					if (forHS) {
						// [UNIREG-1390]
						typeAutoriteFiscale = TypeAutoriteFiscale.PAYS_HS;
					}
					else {
						Assert.isTrue(departHC || forHC);
						typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_HC;
					}
				}
			}
		}
		else {
			typeAutoriteFiscale = fors.principal.getTypeAutoriteFiscale();
		}

		return typeAutoriteFiscale;
	}

	/**
	 * Détermine l'assujettissement pour une période où il est sûr de ne pas avoir de fractionnement.
	 *
	 * @param motifFractDebut le motif de début de fractionnement de l'assujettissement si existant
	 * @param motifFractFin   le motif de fin de fractionnement de l'assujettissement si existant
	 */
	private static Assujettissement determinePeriode(final DecompositionFors fors, final MotifFor motifFractDebut,
	                                                 final MotifFor motifFractFin) throws AssujettissementException {

		RegDate debut = fors.getDateDebut(); // date de début de l'assujettissement (peut changer)
		RegDate fin = fors.getDateFin(); // date de fin de l'assujettissement (peut changer)

		final TypeAutoriteFiscale typeAutoriteFiscale = determineTypeAutoriteFiscale(fors);
		if (typeAutoriteFiscale == null) {
			return null;
		}

		switch (typeAutoriteFiscale) {
		case COMMUNE_OU_FRACTION_VD: {

			Assert.notNull(fors.principal); // purement technique
			final MotifRattachement rattachement = fors.principal.getMotifRattachement();
			final ModeImposition modeImposition = fors.principal.getModeImposition();

			if (MotifRattachement.DIPLOMATE_SUISSE.equals(rattachement)) {
				if (fors.secondairesDansLaPeriode.isEmpty()) {
					// cas standard => diplomate suisse (impôt fédéral direct uniquement)
					return new DiplomateSuisse(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				}
				else {
					// présence de fors secondaires => passage en hors-Suisse (imposé cantonal + fédéral)
					return new HorsSuisse(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				}
			}
			else {
				if (!MotifRattachement.DOMICILE.equals(rattachement)) {
					throw new AssujettissementException("Incohérence des données. Le contribuable = " + fors.contribuable.getNumero()
							+ " à la date = " + fors.getDateFin() + " possède un rattachement de type " + rattachement
							+ " sur un for principal alors que seul le rattachement DOMICILE (éventuellement DIPLOMATE) est accepté.");
				}

				switch (modeImposition) {
				case ORDINAIRE:
					return new VaudoisOrdinaire(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				case MIXTE_137_1:
				case MIXTE_137_2:
					return new SourcierMixte(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				case INDIGENT:
					return new Indigent(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				case DEPENSE:
					return new VaudoisDepense(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				case SOURCE:
					return new SourcierPur(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);
				default:
					throw new AssujettissementException("Mode d'imposition inconnu : " + modeImposition);
				}
			}
		}

		case COMMUNE_HC: {

			final Sourcier source = detecteSourcier(motifFractDebut, motifFractFin, fors);
			if (source != null) {
				return source;
			}

			if (fors.secondairesDansLaPeriode.isEmpty()) {
				return null; // le for principal est hors canton + aucun for secondaire -> pas d'assujettissement
			}

			final MotifFor motifDebut = (motifFractDebut != null ? motifFractDebut : determineMotifDebutForsSecondaires(fors));
			final MotifFor motifFin = (motifFractFin != null ? motifFractFin : determineMotifFinForsSecondaires(fors));

			// La période d'assujettissement en raison d'un rattachement économique s'étend à toute l'année pour tous les types de
			// rattachement (art. 8 al. 6 LI). Sauf en cas de décès, veuvage, départ ou arrivée hors-Suisse, où la durée
			// d'assujettissement est réduite en conséquence.
			// [UNIREG-1360] Ce point a été confirmé par Thierry Declercq le 31 août 2009.
			return new HorsCanton(fors.contribuable, debut, fin, motifDebut, motifFin, fors);
		}

		case PAYS_HS:

			final Sourcier source = detecteSourcier(motifFractDebut, motifFractFin, fors);
			if (source != null) {
				return source;
			}

			if (fors.secondairesDansLaPeriode.isEmpty()) {
				return null; // le for principal est hors Suisse + aucun for secondaire -> pas d'assujettissement
			}

			// La période d'assujettissement est limitée à la période couverte par les fors secondaires
			debut = RegDateHelper.maximum(fors.secondairesDansLaPeriode.getMinDateDebut(), debut, NullDateBehavior.EARLIEST);
			fin = RegDateHelper.minimum(fors.secondairesDansLaPeriode.getMaxDateFin(), fin, NullDateBehavior.LATEST);

			return new HorsSuisse(fors.contribuable, debut, fin, motifFractDebut, motifFractFin, fors);

		default:
			throw new AssujettissementException("Type d'autorité fiscale inconnnue :" + typeAutoriteFiscale);
		}
	}

	/**
	 * Analyse les fors secondaires et retourne un motif d'ouverture si le premier for secondaire a été ouvert dans la période.
	 *
	 * @param fors la décomposition des fors pour la période considérée.
	 * @return un motif d'ouverture ou <i>null</i> si un fors secondaire était déjà ouvert avant la période.
	 */
	private static MotifFor determineMotifDebutForsSecondaires(DecompositionFors fors) {

		final RegDate debut = fors.getDateDebut();

		MotifFor motifDebut = null;
		boolean ouverturePremier = true;

		// détermine le motif d'ouverture le plus fort de tous les fors secondaires ouverts dans la période
		for (ForFiscalSecondaire fs : fors.secondairesDansLaPeriode) {
			if (fs.getDateDebut() != null && fs.getDateDebut().isAfterOrEqual(debut)) {
				final MotifFor motif = fs.getMotifOuverture();
				if (motif != null && (motifDebut == null || motifDebut == MotifFor.VENTE_IMMOBILIER)) {
					motifDebut = motif;
				}
			}
			else {
				ouverturePremier = false;
				break;
			}
		}

		if (ouverturePremier) {
			// le premier fors secondaire a été ouvert dans la période, on retourne le motif d'ouverture
			return motifDebut;
		}
		else {
			// au moins un fors secondaire était ouvert au début de la période -> pas de motif d'ouverture
			return null;
		}
	}

	/**
	 * Analyse les fors secondaires et retourne un motif de fermeture si le dernier for secondaire a été fermé dans la période.
	 *
	 * @param fors la décomposition des fors pour la période considérée.
	 * @return un motif de fermeture ou <i>null</i> s'il reste un for secondaire ouvert à la fin de la période.
	 */
	private static MotifFor determineMotifFinForsSecondaires(DecompositionFors fors) {

		final RegDate fin = fors.getDateFin();

		MotifFor motifFin = null;
		boolean fermetureDernier = true;

		// détermine le motif de fermeture le plus fort de tous les fors secondaires fermés dans la période
		for (ForFiscalSecondaire fs : fors.secondairesDansLaPeriode) {
			if (fs.getDateFin() != null && fs.getDateFin().isBeforeOrEqual(fin)) {
				final MotifFor motif = fs.getMotifFermeture();
				if (motif != null && (motifFin == null || motifFin == MotifFor.VENTE_IMMOBILIER)) {
					motifFin = motif;
				}
			}
			else {
				fermetureDernier = false;
				break;
			}
		}

		if (fermetureDernier) {
			// le dernier fors secondaire a été fermé dans la période, on retourne le motif de fermeture
			return motifFin;
		}
		else {
			// au moins un fors secondaire reste ouvert -> pas de motif de fermeture
			return null;
		}
	}

	/**
	 * Détecte si le contribuable est sourcier (dans les cas hors-canton/hors-Suisse) et retourne son assujettissement si c'est le cas.
	 *
	 * @param fors la décomposition des fors du contribuable
	 * @return l'assujettissement de type source ou <b>null</b> si le contribuable n'est pas sourcier.
	 */
	private static Sourcier detecteSourcier(final MotifFor motifFractDebut, final MotifFor motifFractFin, final DecompositionFors fors)
			throws AssujettissementException {
		if (fors.principal != null) {
			final ModeImposition modeImposition = fors.principal.getModeImposition();
			if (modeImposition == null) {
				String message = "Le mode d'imposition du for principal n°" + fors.principal.getId() + " du contribuable n°"
						+ fors.contribuable.getNumero() + " est nul";
				throw new IllegalArgumentException(message);
			}
			switch (modeImposition) {
			case SOURCE:
				return newSourcierPur(motifFractDebut, motifFractFin, fors);
			case MIXTE_137_1:
				return newSourcierMixte137Al1(motifFractDebut, motifFractFin, fors);
			case MIXTE_137_2:
				throw new AssujettissementException("Incohérence des données. Le contribuable = " + fors.contribuable.getNumero()
						+ " à la date = " + fors.getDateFin() + " possède un mode d'imposition de type "
						+ modeImposition + " mais il est hors-Canton.");
			}
		}
		return null;
	}

	/**
	 * Crée un assujettissement de type sourcier pure.
	 */
	private static Sourcier newSourcierPur(final MotifFor motifFractDebut, final MotifFor motifFractFin, final DecompositionFors fors)
			throws AssujettissementException {

		if (!fors.secondairesDansLaPeriode.isEmpty()) {
			throw new AssujettissementException("Incohérence des données. Le contribuable = " + fors.contribuable.getNumero()
					+ " à la date = " + fors.getDateFin() + " possède un mode d'imposition de type " + fors.principal.getModeImposition()
					+ " et il possède des fors secondaires (sourcier mixte ?).");
		}

		return new SourcierPur(fors.contribuable, fors.getDateDebut(), fors.getDateFin(), motifFractDebut, motifFractFin, fors);
	}

	/**
	 * Crée un assujettissement de type sourcier mixte art. 137 al. 1, c'est-à-dire pour les contribuables qui possèdent un for hors-canton ou hors-Suisse et (obligatoirement) un for secondaire dans le
	 * canton.
	 */
	private static Sourcier newSourcierMixte137Al1(final MotifFor motifFractDebut, final MotifFor motifFractFin,
	                                               final DecompositionFors fors) throws AssujettissementException {

		if (fors.secondaires.isEmpty()) {
			throw new AssujettissementException("Incohérence des données. Le contribuable = " + fors.contribuable.getNumero()
					+ " à la date = " + fors.getDateFin() + " possède un mode d'imposition de type " + fors.principal.getModeImposition()
					+ " mais il ne possède aucun for secondaire (sourcier pur ?).");
		}

		return new SourcierMixte(fors.contribuable, fors.getDateDebut(), fors.getDateFin(), motifFractDebut, motifFractFin, fors);
	}

	public boolean isCollatable(DateRange next) {
		return getClass() == next.getClass() && DateRangeHelper.isCollatable(this, next);
	}

	/**
	 * Un assujettissement est dit "actif" sur une commune donnée si cette commune doit recevoir des sous
	 * par la répartition inter-communale concernant ce contribuable, donc dans le cas d'un contribuable
	 * résident vaudois qui possède un for secondaire sur une autre commune vaudoise, les deux communes
	 * concernées seront considéreées comme "actives".
	 * <p/>
	 * Cette méthode <b>n'est pas appelable</b> sur un assujettissement résultat d'une "collation" car alors les fors sous-jacents
	 * ne sont pas conservés.
	 *
	 * @param noOfsCommune numéro OFS de la commune vaudoise pour laquelle la question est posée
	 * @return vrai si l'assujettissement est actif sur la commune considérée, faux sinon
	 */
	public boolean isActifSurCommune(int noOfsCommune) {

		// très probablement un assujjetissement "collationné"
		Assert.notNull(fors);

		boolean actif = false;
		final ForFiscalPrincipal ffp = (fors.principal != null ? fors.principal : fors.principauxDansLaPeriode.last());
		if (ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getNumeroOfsAutoriteFiscale() == noOfsCommune) {
			actif = true;
		}
		else if (fors.secondairesDansLaPeriode.size() > 0) {
			for (ForFiscalSecondaire ffs : fors.secondairesDansLaPeriode) {
				if (ffs.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffs.getNumeroOfsAutoriteFiscale() == noOfsCommune) {
					actif = true;
					break;
				}
			}
		}
		return actif;
	}
}
