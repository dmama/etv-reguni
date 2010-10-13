package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.Triplet;
import ch.vd.uniregctb.common.TripletIterator;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.Tiers;
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

	private static final Adapter adapter = new Adapter();

	private final Contribuable contribuable;
	private RegDate dateDebut;
	private RegDate dateFin;
	private MotifFor motifDebut;
	private MotifFor motifFin;
	private DecompositionFors fors;

	public Assujettissement(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifDebut, MotifFor motifFin) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.contribuable = contribuable;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.motifDebut = motifDebut;
		this.motifFin = motifFin;
		this.fors = null;
	}

	/**
	 * Permet de construire un assujettissement unique composé de deux assujettissement de même types qui se touchent
	 *
	 * @param courant l'assujettissement courant
	 * @param suivant l'assujettissement suivant
	 */
	protected Assujettissement(Assujettissement courant, Assujettissement suivant) {
		Assert.isTrue(courant.isCollatable(suivant));
		this.contribuable = courant.contribuable;
		this.dateDebut = courant.dateDebut;
		this.dateFin = suivant.dateFin;
		this.motifDebut = courant.motifDebut;
		this.motifFin = suivant.motifFin;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.fors = null;
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	private void setDateDebut(RegDate date) {
		this.dateDebut = date;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	private void setDateFin(RegDate date) {
		this.dateFin = date;
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	public boolean isCollatable(DateRange next) {
		// dans le cas d'un départ HS et d'une arrivée HC, on ne veut pas collater les deux assujettissements
		final boolean departHSEtArriveeHC = (this.getMotifFractFin() == MotifFor.DEPART_HS && ((Assujettissement) next).getMotifFractDebut() == MotifFor.ARRIVEE_HC);
		return getClass() == next.getClass() && DateRangeHelper.isCollatable(this, next) && !departHSEtArriveeHC;
	}

	/**
	 * @return le motif de début de l'assujettissement
	 */
	public MotifFor getMotifFractDebut() {
		return motifDebut;
	}

	/**
	 * @return le motif de fin de l'assujettissement
	 */
	public MotifFor getMotifFractFin() {
		return motifFin;
	}

	public DecompositionFors getFors() {
		if (fors == null) { // lazy init
			fors = new DecompositionForsPeriode(contribuable, dateDebut, dateFin);
		}
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
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement complète.
	 *
	 * @param ctb le contribuable dont on veut déterminer l'assujettissement
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	public static List<Assujettissement> determine(Contribuable ctb) throws AssujettissementException {

		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			return null;
		}

		return determine(ctb, fpt);
	}

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement complète du point de vue de la commune vaudoise dont le numéro OFS
	 * étendu est donné en paramètre
	 * <p/><p/>
	 * <b>ATTENTION:</b> cette méthode n'est pas capable de faire la différence entre un vaudois avec for secondaire sur une commune (celle donnée en paramètre) différente
	 * de la commune de domicile et un hors-canton qui a le même for secondaire... (en d'autres termes : l'assujettissement du vaudois vu de la commune où il a son
	 * for secondaire sera HorsCanton !!)
	 *
	 * @param ctb le contribuable dont on veut déterminer l'assujettissement
	 * @param noOfsCommuneVaudoise le numéro OFS de la commune vaudoise dont on veut le point de vue
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	public static List<Assujettissement> determinePourCommune(Contribuable ctb, int noOfsCommuneVaudoise) throws AssujettissementException {

		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			return null;
		}

		// l'assujettissement ne considère que les fors principaux et secondaires pour le moment
		// (peut-être un jour y aura-t-il aussi les fors autres éléments imposables)

		// pour les fors secondaires (et autres éléments imposables, du coup), c'est facile, tout ce qui n'est
		// pas sur la commune qui nous intéresse peut être enlevé
		retirerForsHorsCommune(fpt.secondaires, noOfsCommuneVaudoise);
		retirerForsHorsCommune(fpt.autreElementImpot, noOfsCommuneVaudoise);

		// les fors principaux vaudois qui ne sont pas sur la commune vaudoise qui nous intéresse vont être remplacés par
		// des fors hors-canton bidons (commune -1)
		filtrerForsPrincipauxHorsCommune(fpt.principaux, noOfsCommuneVaudoise);

		return determine(ctb, fpt);
	}

	private static void filtrerForsPrincipauxHorsCommune(List<ForFiscalPrincipal> liste, int noOfsCommuneVaudoise) {
		final ListIterator<ForFiscalPrincipal> iterPrn = liste.listIterator();
		while (iterPrn.hasNext()) {
			final ForFiscalPrincipal ffp = iterPrn.next();
			if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getNumeroOfsAutoriteFiscale() != noOfsCommuneVaudoise) {
				final ForFiscalPrincipal remplacement = new ForFiscalPrincipal(ffp.getDateDebut(), ffp.getDateFin(), -1, TypeAutoriteFiscale.COMMUNE_HC, ffp.getMotifRattachement(), ffp.getModeImposition());
				iterPrn.set(remplacement);
			}
		}
	}

	private static <T extends ForFiscalRevenuFortune> void retirerForsHorsCommune(List<T> liste, int noOfsCommuneVaudoise) {
		final Iterator<T> iter = liste.iterator();
		while (iter.hasNext()) {
			final T ff = iter.next();
			if (ff.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || ff.getNumeroOfsAutoriteFiscale() != noOfsCommuneVaudoise) {
				iter.remove();
			}
		}
	}

	private static List<Assujettissement> determine(Contribuable ctb, ForsParType fors) throws AssujettissementException {
		try {
			ajouteForsPrincipauxFictifs(fors.principaux);

			// Détermination des données d'assujettissement brutes
			final List<Fraction> fractionnements = new ArrayList<Fraction>();
			final List<Data> domicile = determineAssujettissementDomicile(fors.principaux, fractionnements);
			final List<Data> economique = determineAssujettissementEconomique(fors.secondaires, fractionnements);
			fusionne(domicile, economique);

			// Création des assujettissements finaux
			List<Assujettissement> assujettissements = instanciate(ctb, domicile);
			assujettissements = DateRangeHelper.collate(assujettissements);
			adapteDatesDebutEtFin(assujettissements);

			assertCoherenceRanges(assujettissements);

			return assujettissements.size() == 0 ? null : assujettissements;
		}
		catch (AssujettissementException e) {
			final ValidationResults vr = ctb.validateFors();
			if (vr.hasErrors()) {
				// si le contribuable ne valide pas, on est un peu plus explicite
				throw new AssujettissementException("Une exception a été levée sur le contribuable n°" + ctb.getNumero() + " lors du calcul des assujettissements, mais en fait le contribuable ne valide pas: " + vr.toString(), e);
			}
			else {
				// autrement, on propage simplement l'exception
				throw e;
			}
		}
	}

	/**
	 * Cette méthode ajoute des fors fiscaux principaux fictifs, c'est-à-dire des fors qui devraient exister en fonction des caractéristiques des fors existants, mais qui manquent.
	 *
	 * @param forsPrincipaux les fors principaux (non-fictifs) du contribuable.
	 */
	private static void ajouteForsPrincipauxFictifs(List<ForFiscalPrincipal> forsPrincipaux) {

		List<ForFiscalPrincipal> forsFictifs = null;

		// [UNIREG-2444] si un for fiscal principal possède un motif d'ouverture d'obtention de permis C et qu'il n'y a pas de for fiscal principal précédent,
		// on suppose que le contribuable était sourcier et qu'il y a passage du rôle source pur au rôle ordinaire. Dans les faits pour ne pas casser l'algorithme général,
		// on ajoute artificiellement un for principal avec le mode d'imposition source.
		final TripletIterator<ForFiscalPrincipal> iter = new TripletIterator<ForFiscalPrincipal>(forsPrincipaux.iterator());
		while (iter.hasNext()) {
			final Triplet<ForFiscalPrincipal> triplet = iter.next();
			if (triplet.current.getMotifOuverture() == MotifFor.PERMIS_C_SUISSE && (triplet.previous == null || !DateRangeHelper.isCollatable(triplet.previous, triplet.current))) {
				// On ajoute un for principal source compris entre le début de l'année et la date d'ouverture du for principal courant. Cette période est raccourcie si nécessaire.
				final RegDate debut;
				final RegDate fin = triplet.current.getDateDebut().getOneDayBefore();
				if (triplet.previous == null) {
					debut = RegDate.get(triplet.current.getDateDebut().year(), 1, 1);
				}
				else {
					debut = RegDateHelper.maximum(RegDate.get(triplet.current.getDateDebut().year(), 1, 1), triplet.previous.getDateFin().getOneDayAfter(), NullDateBehavior.EARLIEST);
				}
				final ForFiscalPrincipal forFictif = new ForFiscalPrincipal(debut, fin, triplet.current.getNumeroOfsAutoriteFiscale(),
						triplet.current.getTypeAutoriteFiscale(), triplet.current.getMotifRattachement(), ModeImposition.SOURCE);
				//noinspection deprecation
				forFictif.setMotifOuverture(MotifFor.INDETERMINE);
				forFictif.setMotifFermeture(MotifFor.PERMIS_C_SUISSE);
				if (forsFictifs == null) {
					forsFictifs = new ArrayList<ForFiscalPrincipal>();
				}
				forsFictifs.add(forFictif);
			}
		}
		
		if (forsFictifs != null) {
			forsPrincipaux.addAll(forsFictifs);
			Collections.sort(forsPrincipaux, new DateRangeComparator<ForFiscalPrincipal>());
		}
	}

	/**
	 * Analyse les fors du contribuable et construit la liste des périodes d'assujettissement durant l'année spécifiée. Dans la grande majorité des cas, il n'y a qu'une seule période d'assujettissement
	 * et elle coïncide avec l'année civile. Dans certains cas rares, il peut y avoir deux - voire même plus que de deux - périodes d'assujettissement distinctes.
	 *
	 * @param contribuable le contribuable dont on veut déterminer l'assujettissement
	 * @param annee        l'année correspondant à la période fiscale considérée (du 1er janvier au 31 décembre)
	 * @return une liste d'assujettissement contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	public static List<Assujettissement> determine(Contribuable contribuable, int annee) throws AssujettissementException {

		List<Assujettissement> list = determine(contribuable);
		if (list == null) {
			return null;
		}

		list = DateRangeHelper.extract(list, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), adapter);
		if (list.isEmpty()) {
			list = null;
		}

		return list;
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
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	public static List<Assujettissement> determine(Contribuable contribuable, final DateRange range, boolean collate) throws AssujettissementException {

		List<Assujettissement> list = determine(contribuable);
		if (list == null) {
			return null;
		}

		if (!collate) {
			Assert.notNull(range);
			list = split(list, range.getDateDebut().year(), range.getDateFin().year());
		}

		if (range != null) {
			// Limitation des assujettissements au range demandé
			list = DateRangeHelper.extract(list, range.getDateDebut(), range.getDateFin(), adapter);
		}

		if (list.isEmpty()) {
			list = null;
		}
		return list;
	}

	/**
	 * Asserte que les ranges ne se chevauchent pas.
	 *
	 * @param ranges les ranges à tester
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static void assertCoherenceRanges(List<? extends DateRange> ranges) throws AssujettissementException {
		DateRange previous = null;
		for (DateRange current : ranges) {
			if (previous != null) {
				if (DateRangeHelper.intersect(previous, current)) {
					throw new AssujettissementException("L'assujettissement [" + previous + "] entre en collision avec le suivant [" + current + "]");
				}
			}
			previous = current;
		}
	}

	private static class Fraction {
		public final RegDate date;
		public final MotifFor motif;

		private Fraction(RegDate date, MotifFor motif) {
			Assert.notNull(date);
			this.date = date;
			this.motif = motif;
		}
	}

	/**
	 * Détermine les données d'assujettissements brutes pour les rattachements de type domicile.
	 *
	 * @param principaux      les fors principaux d'un contribuable
	 * @param fractionnements une liste vide qui contiendra les fractionnements calculés après l'exécution de la méthode
	 * @return la liste des assujettissements brutes calculés
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static List<Data> determineAssujettissementDomicile(List<ForFiscalPrincipal> principaux, List<Fraction> fractionnements) throws AssujettissementException {

		final List<Data> domicile = new ArrayList<Data>();

		RegDate fraction = null; // la dernière date connue de fractionnement de l'assujettissement
		MotifFor motifFraction = null;

		// Détermine les assujettissements pour le rattachement de type domicile
		TripletIterator<ForFiscalPrincipal> iter = new TripletIterator<ForFiscalPrincipal>(principaux.iterator());
		while (iter.hasNext()) {
			final Triplet<ForFiscalPrincipal> triplet = iter.next();

			// on détermine les fors principaux qui précédent et suivent immédiatement
			final ForFiscalPrincipal current = triplet.current;
			final ForFiscalPrincipal previous = (triplet.previous != null && DateRangeHelper.isCollatable(triplet.previous, current) ? triplet.previous : null);
			final ForFiscalPrincipal next = (triplet.next != null && DateRangeHelper.isCollatable(current, triplet.next) ? triplet.next : null);

			// on détecte une éventuelle date de fractionnement à l'ouverture
			if (isFractionOuverture(current, previous)) {
				fraction = current.getDateDebut();
				motifFraction = current.getMotifOuverture();

				if (next != null && isArriveeHCApresDepartHSMemeAnnee(current)) {
					// dans ce cas précis, on veut utiliser le motif d'ouverture du for suivant comme motif de fractionnement
					motifFraction = next.getMotifOuverture();
				}

				fractionnements.add(new Fraction(fraction, motifFraction));
			}

			// on détermine l'assujettissement pour le for principal courant
			final Data a = determine(current, previous, next);
			if (a != null) {

				if (fraction != null && fraction.isAfterOrEqual(a.debut)) {
					a.debut = fraction;
					a.motifDebut = motifFraction;
				}

				domicile.add(a);
			}

			// on détecte une éventuelle date de fractionnement à la fermeture
			if (isFractionFermeture(current, next)) {
				fraction = (current.getDateFin() == null ? null : current.getDateFin().getOneDayAfter());
				motifFraction = current.getMotifFermeture();

				fractionnements.add(new Fraction(fraction, motifFraction));
			}
		}

		return domicile;
	}

	/**
	 * Détermine les données d'assujettissements brutes pour les rattachements de type économique.
	 *
	 * @param secondaires     les fors secondaires d'un contribuable
	 * @param fractionnements la liste des fractionnements d'assujettissement calculés lors de l'analyse des fors principaux
	 * @return la liste des assujettissements brutes calculés
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static List<Data> determineAssujettissementEconomique(List<ForFiscalSecondaire> secondaires, List<Fraction> fractionnements) throws AssujettissementException {
		List<Data> economique = new ArrayList<Data>();
		// Détermine les assujettissements pour le rattachement de type économique
		for (ForFiscalSecondaire f : secondaires) {
			final Data a = determine(f, fractionnements);
			if (a != null) {
				economique.add(a);
			}
		}
		return economique;
	}

	/**
	 * Découpe les assujettissements spécifié année par année pour les années spécifiées
	 *
	 * @param list      les assujettissements à découper
	 * @param startYear l'année de départ (comprise)
	 * @param endYear   l'année de fin (comprise
	 * @return une liste d'assujettissements découpés année par année
	 */
	private static List<Assujettissement> split(List<Assujettissement> list, int startYear, int endYear) {

		List<Assujettissement> splitted = new ArrayList<Assujettissement>();

		// split des assujettissement par années
		for (int year = startYear; year <= endYear; ++year) {
			List<Assujettissement> extracted = DateRangeHelper.extract(list, RegDate.get(year, 1, 1), RegDate.get(year, 12, 31), adapter);
			splitted.addAll(extracted);
		}

		return splitted;
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
	 * Les dates de début/fin d'un assujettissement sont adaptées si les conditions suivantes sont respectées: <ul> <li>l'assujettissement est de type source pure</li> <li>l'assujettissement
	 * précédent/suivant est de type ordinaire non-mixte (ordinaire, dépense, ... mais pas sourcier mixte)</li> </ul>
	 * <p/>
	 * En gros, à chaque passage source pure à ordinaire et vice-versa les dates de début et fin sont adaptées.
	 *
	 * @param assujettissements une liste des assujettissement
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
			final MotifFor motifDebut = courant.getMotifFractDebut();
			final MotifFor motifFin = courant.getMotifFractFin();

			// faut-il adapter la date de début ?
			final Assujettissement precedent = triplet.previous;
			if ((precedent == null || !(precedent instanceof Sourcier)) && !isDepartOuArriveeHorsSuisse(motifDebut)) { // [UNIREG-2155]
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
			if (fin != null && !isDepartOuArriveeHorsSuisse(motifFin)) { // [UNIREG-2155]
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
		}

		return res;
	}

	private static boolean isDepartOuArriveeHorsSuisse(MotifFor motif) {
		return motif == MotifFor.DEPART_HS || motif == MotifFor.ARRIVEE_HS;
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
		if (suivant.getDateFin() == null || newDateDebut.isBeforeOrEqual(suivant.getDateFin())) {
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

	private static boolean is31Decembre(RegDate date) {
		return date.month() == 12 && date.day() == 31;
	}

	private static boolean roleSourcierPur(ForFiscalPrincipal forPrecedent) {
		return forPrecedent.getModeImposition() == ModeImposition.SOURCE;
	}

	private static boolean roleSourcierMixte(ForFiscalPrincipal forPrecedent) {
		return forPrecedent.getModeImposition() == ModeImposition.MIXTE_137_1 || forPrecedent.getModeImposition() == ModeImposition.MIXTE_137_2;
	}

	private static boolean roleOrdinaireNonMixte(ForFiscalPrincipal forCourant) {
		final ModeImposition mode = forCourant.getModeImposition();
		return !ModeImposition.SOURCE.equals(mode) && !ModeImposition.MIXTE_137_1.equals(mode) && !ModeImposition.MIXTE_137_2.equals(mode);
	}

	/**
	 * Détermine s'il y a un départ ou une arrivée hors-Suisse entre le deux fors fiscaux spécifiés. Cette méthode s'assure que les types d'autorité fiscales sont cohérentes de manière à détecter les
	 * faux départs/arrivées hors-Suisse dûs à des motifs d'ouverture/fermetures incohérents.
	 * <p/>
	 * <b>Note:</b> si les deux fors sont renseignés, ils doivent se toucher.
	 *
	 * @param left  le for fiscal de gauche (peut être nul)
	 * @param right le for fiscal de droite (peut être nul)
	 * @return <b>true</b> si un départ ou une arrivée hors-Suisse est détecté.
	 */
	private static boolean isDepartOuArriveeHorsSuisse(ForFiscalPrincipal left, ForFiscalPrincipal right) {
		Assert.isFalse(left == null && right == null);

		final boolean fraction;

		if (left != null && right != null && left.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE && right.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE) {
			Assert.isTrue(left.getDateFin().getOneDayAfter() == right.getDateDebut());

			//noinspection SimplifiableIfStatement
			if (isArriveeHCApresDepartHSMemeAnnee(left)) {
				// dans le cas d'un départ HS et d'arrivée HC dans le même année (donc avec un seul for fiscal HS avec
				// ces deux motifs), il ne faut pas que l'arrivée HC fractionne l'assujettissement.
				fraction = false;
			}
			else {
				// dans tous les autres cas, on ignore les motifs d'ouverture/fermeture (qui sont souvent faux) et
				// on se base uniquement sur les autorités fiscales (qui sont des valeurs sûres)
				fraction = (left.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS || right.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) &&
						(left.getTypeAutoriteFiscale() != right.getTypeAutoriteFiscale());
			}
		}
		else {
			// Autrement, on se base sur les motifs d'ouverture et de fermeture
			boolean motifDetecte = (left != null && (left.getMotifFermeture() == MotifFor.ARRIVEE_HS || left.getMotifFermeture() == MotifFor.DEPART_HS));
			motifDetecte = motifDetecte || (right != null && (right.getMotifOuverture() == MotifFor.ARRIVEE_HS || right.getMotifOuverture() == MotifFor.DEPART_HS));

			fraction = motifDetecte;
		}

		return fraction;
	}

	private static boolean isArriveeHCApresDepartHSMemeAnnee(ForFiscalPrincipal left) {
		return left.getDateDebut().year() == left.getDateFin().year() && left.getMotifOuverture() == MotifFor.DEPART_HS && left.getMotifFermeture() == MotifFor.ARRIVEE_HC;
	}

	/**
	 * [UNIREG-2759] Détermine si le for fiscal se ferme avec un départ hors-canton la même année qu'une arrivée de hors-Suisse
	 *
	 * @param ffp une for fiscal principal
	 * @return <b>vrai</b> si le for fiscal se ferme avec un départ hors-canton la même année qu'une arrivée de hors-Suisse; <b>faux</b> autrement.
	 */
	private static boolean isDepartHCApresArriveHSMemeAnnee(ForFiscalPrincipal ffp) {
		if (ffp == null) {
			return false;
		}
		final RegDate fin = ffp.getDateFin();
		final MotifFor motifFin = ffp.getMotifFermeture();
		return fin != null && motifFin == MotifFor.DEPART_HC && fin.year() == ffp.getDateDebut().year();
	}

	/**
	 * @param left  le for fiscal de gauche (peut être nul)
	 * @param right le for fiscal de droite (peut être null)
	 * @return <b>true</b> si un départ ou une arrivée hors-canton est détectée entre les forts fiscaux spécifiés. Cette méthode s'assure que les types d'autorité fiscales sont cohérentes de manière à
	 *         détecter les faux départs/arrivées hors-canton.
	 */
	private static boolean isDepartOuArriveeHorsCanton(ForFiscalPrincipal left, ForFiscalPrincipal right) {
		Assert.isFalse(left == null && right == null);

		boolean motifDetecte = (left != null && (left.getMotifFermeture() == MotifFor.ARRIVEE_HC || left.getMotifFermeture() == MotifFor.DEPART_HC));
		motifDetecte = motifDetecte || (right != null && (right.getMotifOuverture() == MotifFor.ARRIVEE_HC || right.getMotifOuverture() == MotifFor.DEPART_HC));

		boolean typeAutoriteOk = (left == null || left.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC);
		typeAutoriteOk = typeAutoriteOk || (right == null || right.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC);

		return motifDetecte && typeAutoriteOk;
	}

	private static boolean isFractionOuverture(ForFiscalPrincipal current, ForFiscalPrincipal previous) {

		final MotifFor motifOuverture = current.getMotifOuverture();
		final ModeImposition modeImposition = current.getModeImposition();

		final ModeImposition previousModeImposition = (previous == null ? null : previous.getModeImposition());

		boolean fraction = false;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = true;
		}
		else if (isDepartOuArriveeHorsSuisse(previous, current) && isDepartDepuisOuArriveeVersVaud(current, previous) && !isDepartHCApresArriveHSMemeAnnee(current)) {
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			fraction = true;
		}
		else if (isDepartOuArriveeHorsCanton(previous, current) &&
				(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 ||
						previousModeImposition == ModeImposition.SOURCE || previousModeImposition == ModeImposition.MIXTE_137_2)) {
			// [UNIREG-1742] Le départ ou l'arrivée hors-Canton d'un sourcier pur ou mixte 137 al. 2 (donc sans for secondaire) doit provoquer un fractionnement
			fraction = true;
		}
		else if (previous != null && ((roleSourcierPur(previous) && roleOrdinaireNonMixte(current)) || (roleOrdinaireNonMixte(previous) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle ordinaire non-mixte (et vice versa) doit provoquer un fractionnement.
			fraction = true;
		}
		else if (previous != null && current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS &&
				((roleSourcierPur(previous) && roleSourcierMixte(current)) || (roleSourcierMixte(previous) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle source-mixte (et vice versa) doit provoquer un fractionnement (hors-Suisse uniquement).
			fraction = true;
		}

		return fraction;
	}

	private static boolean isFractionFermeture(ForFiscalPrincipal current, ForFiscalPrincipal next) {

		final MotifFor motifFermeture = current.getMotifFermeture();
		final ModeImposition modeImposition = current.getModeImposition();

		final ModeImposition nextModeImposition = (next == null ? null : next.getModeImposition());

		boolean fraction = false;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = true;
		}
		else if (isDepartOuArriveeHorsSuisse(current, next) && isDepartDepuisOuArriveeVersVaud(current, next) && !isDepartHCApresArriveHSMemeAnnee(next)) {
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			fraction = true;
		}
		else if (isDepartOuArriveeHorsCanton(current, next) &&
				(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 ||
						nextModeImposition == ModeImposition.SOURCE || nextModeImposition == ModeImposition.MIXTE_137_2)) {
			// [UNIREG-1742] Le départ ou l'arrivée hors-Canton d'un sourcier pur ou mixte 137 al. 2 (donc sans for secondaire) doit provoquer un fractionnement
			fraction = true;
		}
		else if (next != null && ((roleSourcierPur(next) && roleOrdinaireNonMixte(current)) || (roleOrdinaireNonMixte(next) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle ordinaire non-mixte (et vice versa) doit provoquer un fractionnement.
			fraction = true;
		}
		else if (next != null && current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS &&
				((roleSourcierPur(next) && roleSourcierMixte(current)) || (roleSourcierMixte(next) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle source-mixte (et vice versa) doit provoquer un fractionnement (hors-Suisse uniquement).
			fraction = true;
		}

		return fraction;
	}

	/**
	 * Détermine la date de début d'un assujettissement induit par un for fiscal principal.
	 *
	 * @param current  le for fiscal principal à la source de l'assujettissement
	 * @param previous le for fiscal principal immédiatement précédant le for courant; ou <b>null</b> s'il n'y en a pas
	 * @return la date de début de l'assujettissement
	 */
	private static RegDate determineDateDebutAssujettissement(ForFiscalPrincipal current, ForFiscalPrincipal previous) {

		final RegDate debut;
		if (isFractionOuverture(current, previous) || current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			debut = current.getDateDebut();
		}
		else {
			// dans tous les autres cas, l'assujettissement débute au 1er janvier de l'année courante
			debut = getDernier1Janvier(current.getDateDebut());
		}

		return debut;
	}

	/**
	 * Détermine la date de fin d'un assujettissement induit par un for fiscal principal.
	 *
	 * @param current le for fiscal principal à la source de l'assujettissement
	 * @param next    le for fiscal principal immédiatement suivant le for courant; ou <b>null</b> s'il n'y en a pas
	 * @return la date de fin de l'assujettissement
	 */
	private static RegDate determineDateFinAssujettissement(ForFiscalPrincipal current, ForFiscalPrincipal next) {

		final RegDate fin = current.getDateFin();

		final RegDate afin;
		if (fin == null || isFractionFermeture(current, next) || current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			afin = fin;
		}
		else {
			// dans tous les autres cas, l'assujettissement finit à la fin de l'année précédente
			afin = getDernier31Decembre(fin);
		}

		return afin;
	}

	/**
	 * Détermine la date de début de la période de non-assujettissement correspondant à un for fiscal principal (période durant laquelle un for secondaire pourrait provoquer un assujettissement).
	 *
	 * @param current  le for fiscal principal courant
	 * @param previous le for fiscal principal immédiatement précédant le for courant; ou <b>null</b> s'il n'y en a pas
	 * @return la date de fin de la période de non-assujettissement
	 */
	private static RegDate determineDateDebutNonAssujettissement(ForFiscalPrincipal current, ForFiscalPrincipal previous) {

		final RegDate debut = current.getDateDebut();

		final RegDate adebut;
		if (isDepartOuArriveeHorsSuisse(previous, current)) {
			if (isDepartDepuisOuArriveeVersVaud(current, previous) && !isDepartHCApresArriveHSMemeAnnee(current)) {
				// fin de l'assujettissement en cours de période fiscale (fractionnement)
				adebut = debut;
			}
			else {
				// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
				// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
				adebut = getDernier1Janvier(debut);
			}
		}
		else if (previous != null && ((roleSourcierPur(previous) && roleSourcierMixte(current)) || (roleSourcierMixte(previous) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle source-mixte (et vice versa) doit provoquer un fractionnement.
			adebut = debut;
		}
		else if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && current.getMotifOuverture() == MotifFor.DEPART_HC) {
			// cas limite du ctb qui part HC et arrive de HS dans la même année -> la durée précise de la période hors-Suisse n'est pas connue et on prend la solution
			// la plus avantageuse pour l'ACI : arrivée de HS au 1er janvier de l'année suivante
			adebut = RegDate.get(debut.year() + 1, 1, 1);
		}
		else {
			// dans tous les autres cas, l'assujettissement débute au 1er janvier de l'année courante
			adebut = getDernier1Janvier(debut);
		}

		return adebut;
	}

	/**
	 * Détermine la date de fin de la période de non-assujettissement correspondant à un for fiscal principal (période durant laquelle un for secondaire pourrait provoquer un assujettissement).
	 *
	 * @param current le for fiscal principal courant
	 * @param next    le for fiscal principal immédiatement suivant le for courant; ou <b>null</b> s'il n'y en a pas
	 * @return la date de fin de la période de non-assujettissement
	 */
	private static RegDate determineDateFinNonAssujettissement(ForFiscalPrincipal current, ForFiscalPrincipal next) {

		final RegDate fin = current.getDateFin();
		final MotifFor motifFermeture = current.getMotifFermeture();

		final RegDate afin;
		if (fin == null) {
			afin = null;
		}
		else if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			afin = fin;
		}
		else if (isDepartOuArriveeHorsSuisse(current, next)) {
			if (isDepartDepuisOuArriveeVersVaud(current, next) && !isDepartHCApresArriveHSMemeAnnee(next)) {
				// fin de l'assujettissement en cours de période fiscale (fractionnement)
				afin = fin;
			}
			else {
				// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
				// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
				afin = getDernier31Decembre(fin);
			}
		}
		else if (next != null && ((roleSourcierPur(next) && roleSourcierMixte(current)) || (roleSourcierMixte(next) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle source-mixte (et vice versa) doit provoquer un fractionnement.
			afin = fin;
		}
		else if (isArriveeHCApresDepartHSMemeAnnee(current)) {
			// cas limite du ctb qui part HS et arrive de HC dans la même année -> la durée précise de la période hors-Suisse n'est pas connue et on prend la solution
			// la plus avantageuse pour l'ACI : arrivée de HS au 31 décembre de l'année précédente.
			afin = getDernier31Decembre(fin);
		}
		else if (next == null) {
			// si le for secondaire se ferme mais qu'il n'y a pas de for immédiatement suivant (par exemple: cas du contribuable hors-canton avec immeuble, qui vend son immeuble et
			// dont le for principal hors-canton est fermé à la date de vente), alors il s'agit d'une "fausse" fermeture du for et on le considère valide jusqu'à la fin de l'année.
			if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
				afin = getProchain31Decembre(fin); // le rattachement économique s'étend à toute l'année pour le HC
			}
			else {
				afin = fin; // le rattachement économmique est limité à la période de validité du for pour les HS
			}
		}
		else {
			// dans tous les autres cas, l'assujettissement finit à la fin de l'année précédente
			afin = getDernier31Decembre(fin);
		}

		return afin;
	}

	/**
	 * @param date une date
	 * @return le 31 décembre le plus proche de la date spécifiée et qui ne soit pas dans le futur.
	 */
	private static RegDate getDernier31Decembre(RegDate date) {
		final RegDate afin;
		if (is31Decembre(date)) {
			afin = date;
		}
		else {
			afin = RegDate.get(date.year() - 1, 12, 31);
		}
		return afin;
	}

	private static RegDate getDernier1Janvier(RegDate date) {
		return RegDate.get(date.year(), 1, 1);
	}

	/**
	 * @param date une date
	 * @return le 31 décembre le plus proche de la date spécifiée et qui ne soit pas dans le passé.
	 */
	private static RegDate getProchain31Decembre(RegDate date) {
		return RegDate.get(date.year(), 12, 31);
	}

	/**
	 * Un assujettissement est dit "actif" sur une commune donnée si cette commune doit recevoir des sous par la répartition inter-communale concernant ce contribuable, donc dans le cas d'un contribuable
	 * résident vaudois qui possède un for secondaire sur une autre commune vaudoise, les deux communes concernées seront considéreées comme "actives".
	 * <p/>
	 * Cette méthode <b>n'est pas appelable</b> sur un assujettissement résultat d'une "collation" car alors les fors sous-jacents ne sont pas conservés.
	 *
	 * @param noOfsCommune numéro OFS de la commune vaudoise pour laquelle la question est posée
	 * @return vrai si l'assujettissement est actif sur la commune considérée, faux sinon
	 */
	public boolean isActifSurCommune(int noOfsCommune) {

		final DecompositionFors fors = getFors();

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

	private enum Type {
		VaudoisOrdinaire,
		VaudoisDepense,
		SourcierMixte,
		SourcierPur,
		Indigent,
		HorsSuisse,
		HorsCanton,
		DiplomateSuisse,
		NonAssujetti
	}

	/**
	 * Représente les données brutes d'une période d'assujettissement
	 */
	private static class Data implements DateRange {

		RegDate debut;
		RegDate fin;
		MotifFor motifDebut;
		MotifFor motifFin;
		Type type;
		TypeAutoriteFiscale typeAut;

		private Data(RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin, Type type, TypeAutoriteFiscale typeAut) {
			this.debut = debut;
			this.fin = fin;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
			this.type = type;
			this.typeAut = typeAut;
		}

		private Data(Data right) {
			this.debut = right.debut;
			this.fin = right.fin;
			this.motifDebut = right.motifDebut;
			this.motifFin = right.motifFin;
			this.type = right.type;
			this.typeAut = right.typeAut;
		}

		public boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, debut, fin, NullDateBehavior.LATEST);
		}

		public RegDate getDateDebut() {
			return debut;
		}

		public RegDate getDateFin() {
			return fin;
		}

		/**
		 * Cette méthode fusionne une donnée d'assujettissement <i>domicile</i> avec une donnée d'assujettissement <i>économique</i>.
		 *
		 * @param eco une donnée d'assujettissement économique
		 * @return une liste de données d'assujettissement résultante qui doivent remplacer l'assujettissement courant; ou <b>null</b> s'il n'y a rien à faire.
		 */
		public List<Data> merge(Data eco) {

			if (type != Type.NonAssujetti) {
				// l'assujettissement 'this' (qui est forcéement de type domicile) est différent de non-assujetti : quelque soit la valeur de 'eco', il prime sur ce dernier et il n'y a rien à faire.
				return null;
			}

			if (!DateRangeHelper.intersect(this, eco)) {
				// pas d'intersection -> rien à faire
				return null;
			}

			final List<Data> list;
			if (eco.debut.isBeforeOrEqual(this.debut) && RegDateHelper.isAfterOrEqual(eco.fin, this.fin, NullDateBehavior.LATEST)) {
				// eco dépasse des deux côtés de this -> pas besoin de découper quoique ce soit
				list = null;
				if (eco.debut == this.debut && this.motifDebut == null) {
					this.motifDebut = eco.motifDebut;
				}
				if (eco.fin == this.fin && this.motifFin == null) {
					this.motifFin = eco.motifFin;
				}
			}
			else {
				list = new ArrayList<Data>(3);
				list.add(this);
				if (eco.debut.isAfter(this.debut)) {
					// on découpe à gauche
					Data left = new Data(this);
					left.fin = eco.debut.getOneDayBefore();
					left.motifFin = eco.motifDebut;
					list.add(0, left);

					// on décale la date de début
					this.debut = eco.debut;
					this.motifDebut = eco.motifDebut;
				}

				if (!RegDateHelper.isAfterOrEqual(eco.fin, this.fin, NullDateBehavior.LATEST)) {
					// on découpe à droite
					Data right = new Data(this);
					right.debut = eco.fin.getOneDayAfter();
					right.motifDebut = eco.motifFin;
					list.add(right);

					// on décale la date de fin
					this.fin = eco.fin;
					this.motifFin = eco.motifFin;
				}

				if (this.debut == eco.debut && this.motifDebut == null) {
					// si le motif de début manque, on profite de celui du for économique pour le renseigner
					this.motifDebut = eco.motifDebut;
				}

				if (this.fin == eco.fin && this.motifFin == null) {
					// si le motif de fin manque, on profite de celui du for économique pour le renseigner
					this.motifFin = eco.motifFin;
				}
			}

			// pas assujetti + immeuble/activité indépendante = hors-canton ou hors-Suisse
			this.type = getAType(this.typeAut);

			return list;
		}
	}

	/**
	 * Détermine les données d'assujettissement pour un for fiscal principal.
	 *
	 * @param current  le for fiscal dont on veut calculer l'assujettissement
	 * @param previous le for fiscal qui précède immédiatement
	 * @param next     le for fiscal qui suit immédiatement
	 * @return les données d'assujettissement, ou <b>null</b> si le for principal n'induit aucun assujettissement
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static Data determine(ForFiscalPrincipal current, ForFiscalPrincipal previous, ForFiscalPrincipal next) throws AssujettissementException {

		final Data data;

		switch (current.getTypeAutoriteFiscale()) {
		case COMMUNE_OU_FRACTION_VD: {

			final RegDate adebut = determineDateDebutAssujettissement(current, previous);
			final RegDate afin = determineDateFinAssujettissement(current, next);

			if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
				final MotifRattachement motifRattachement = current.getMotifRattachement();
				if (motifRattachement == MotifRattachement.DIPLOMATE_SUISSE) {
					// cas particulier du diplomate suisse basé à l'étranger
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), Type.DiplomateSuisse, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				}
				else { // cas général
					if (motifRattachement != MotifRattachement.DOMICILE) {
						throw new AssujettissementException("Le contribuable n°" + current.getTiers().getNumero() + " possède un for fiscal principal avec un motif de rattachement [" +
								motifRattachement + "] ce qui est interdit.");
					}
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), getAType(current.getModeImposition()), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				}
			}
			else {
				// pas d'assujettissement
				data = null;
			}

			break;
		}
		case COMMUNE_HC:
		case PAYS_HS: {

			if (isSource(current.getModeImposition())) {

				final Type type = getAType(current.getModeImposition());
				final RegDate adebut = determineDateDebutAssujettissement(current, previous);
				final RegDate afin = determineDateFinAssujettissement(current, next);

				if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), type, current.getTypeAutoriteFiscale());
				}
				else {
					// pas d'assujettissement
					data = null;
				}
			}
			else {
				final RegDate adebut = determineDateDebutNonAssujettissement(current, previous);
				final RegDate afin = determineDateFinNonAssujettissement(current, next);

				if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), Type.NonAssujetti, current.getTypeAutoriteFiscale());
				}
				else {
					// pas d'assujettissement
					data = null;
				}
			}
			break;
		}

		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + current.getTypeAutoriteFiscale() + "]");
		}

		return data;
	}

	private static boolean isSource(ModeImposition modeImposition) {
		return modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_1 || modeImposition == ModeImposition.MIXTE_137_2;
	}

	private static Data determine(ForFiscalSecondaire ffs, List<Fraction> fractionnements) throws AssujettissementException {

		final RegDate debut = ffs.getDateDebut();
		final RegDate fin = ffs.getDateFin();

		// La période d'assujettissement en raison d'un rattachement économique s'étend à toute l'année pour tous les types de
		// rattachement (art. 8 al. 6 LI). Sauf en cas de décès, veuvage, départ ou arrivée hors-Suisse, où la durée
		// d'assujettissement est réduite en conséquence.
		// [UNIREG-1360] Ce point a été confirmé par Thierry Declercq le 31 août 2009.

		RegDate adebut;
		if (isMariageOuDivorce(ffs.getMotifOuverture())) {
			// [UNIREG-2432] Exception : si le motif d'ouverture est MARIAGE ou SEPARATION, la date de début est ramenée au 1 janvier de l'année courante.
			// L'idée est que dans ces cas-là, le rattachement est transféré de la PP vers le ménage (ou inversément) sur l'entier de la période.
			adebut = getDernier1Janvier(debut);
		}
		else if (isHorsSuisse(ffs.getTiers(), debut)) {
			adebut = debut;
		}
		else {
			adebut = getDernier1Janvier(debut);
		}

		RegDate afin;
		if (fin == null) {
			afin = fin;
		}
		else if (isMariageOuDivorce(ffs.getMotifFermeture())) {
			// [UNIREG-2432] Exception : si le motif de fermeture est MARIAGE ou SEPARATION, la date de fin est ramenée au 31 décembre de l'année précédente.
			// L'idée est que dans ces cas-là, le rattachement est transféré de la PP vers le ménage (ou inversément) sur l'entier de la période.
			afin = getDernier31Decembre(fin);
		}
		else if (isHorsSuisse(ffs.getTiers(), fin)) {
			afin = fin;
		}
		else {
			afin = getProchain31Decembre(fin);
		}

		// Dans tous les cas, si on trouve une date de fractionnement entre la date réelle du début du for et la date de début de l'assujettissement,
		// on adapte cette dernière en conséquence. Même chose pour les dates de fin d'assujettissement.
		for (Fraction f : fractionnements) {
			if (RegDateHelper.isBetween(f.date, adebut, debut, NullDateBehavior.LATEST)) {
				adebut = f.date;
			}

			if (fin != null && RegDateHelper.isBetween(f.date.getOneDayBefore(), fin, afin, NullDateBehavior.LATEST)) {
				afin = f.date.getOneDayBefore();
			}
		}

		if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) { // [UNIREG-2559]
			return new Data(adebut, afin, ffs.getMotifOuverture(), ffs.getMotifFermeture(), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		}
		else {
			// pas d'assujettissement
			return null;
		}
	}

	private static boolean isMariageOuDivorce(MotifFor motif) {
		return motif == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT || motif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
	}

	private static boolean isHorsSuisse(Tiers tiers, RegDate date) {
		Set<ForFiscal> fors = tiers.getForsFiscaux();
		for (ForFiscal f : fors) {
			if (f.isPrincipal() && f.isValidAt(date)) {
				return f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
			}
		}
		return false;
	}

	/**
	 * Fusionne les assujettissements économiques aux assujettissement domicile en appliquant les régles métier. Le résultat est stocké dans la liste des assujettissements domicile.
	 *
	 * @param domicile   les assujettissements pour motif de rattachement domicile
	 * @param economique les assujettissements pour motif de rattachement économique
	 */
	private static void fusionne(List<Data> domicile, List<Data> economique) {

		for (int i = 0; i < domicile.size(); i++) {
			for (Data e : economique) {
				final Data d = domicile.get(i);
				final List<Data> sub = d.merge(e);
				if (sub != null) {
					domicile.set(i, sub.get(0));
					for (int j = 1; j < sub.size(); ++j) {
						domicile.add(i + j, sub.get(j));
					}
				}
			}
		}

		Collections.sort(domicile, new DateRangeComparator<DateRange>());
	}

	private static List<Assujettissement> instanciate(Contribuable ctb, List<Data> all) {
		final List<Assujettissement> assujettissements = new ArrayList<Assujettissement>(all.size());
		for (Data a : all) {
			final Assujettissement assujettissement;
			switch (a.type) {
			case VaudoisOrdinaire:
				assujettissement = new VaudoisOrdinaire(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case VaudoisDepense:
				assujettissement = new VaudoisDepense(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case Indigent:
				assujettissement = new Indigent(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case HorsCanton:
				assujettissement = new HorsCanton(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case HorsSuisse:
				assujettissement = new HorsSuisse(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case SourcierPur:
				assujettissement = new SourcierPur(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut);
				break;
			case SourcierMixte:
				assujettissement = new SourcierMixte(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut);
				break;
			case DiplomateSuisse:
				assujettissement = new DiplomateSuisse(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case NonAssujetti:
				assujettissement = null;
				break;
			default:
				throw new IllegalArgumentException("Type inconnu = [" + a.type + "]");
			}
			if (assujettissement != null) {
				assujettissements.add(assujettissement);
			}
		}
		return assujettissements;
	}

	private static Type getAType(TypeAutoriteFiscale typeAutoriteFiscale) {
		return (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS ? Type.HorsSuisse : Type.HorsCanton);
	}

	private static boolean isDepartDepuisOuArriveeVersVaud(ForFiscalPrincipal left, ForFiscalPrincipal right) {
		// un des deux fors fiscaux doit être dans le canton de Vaud
		return (left != null && left.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) ||
				(right != null && right.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
	}

	private static Type getAType(ModeImposition modeImposition) throws AssujettissementException {
		final Type type;
		switch (modeImposition) {
		case ORDINAIRE:
			type = Type.VaudoisOrdinaire;
			break;
		case MIXTE_137_1:
		case MIXTE_137_2:
			type = Type.SourcierMixte;
			break;
		case INDIGENT:
			type = Type.Indigent;
			break;
		case DEPENSE:
			type = Type.VaudoisDepense;
			break;
		case SOURCE:
			type = Type.SourcierPur;
			break;
		default:
			throw new AssujettissementException("Mode d'imposition inconnu : " + modeImposition);
		}
		return type;
	}

	private static class Adapter implements DateRangeHelper.AdapterCallback<Assujettissement> {
		public Assujettissement adapt(Assujettissement assujettissement, RegDate debut, RegDate fin) {
			if (debut != null && assujettissement.dateDebut != debut) {
				assujettissement.dateDebut = debut;
				assujettissement.motifDebut = null;
			}
			if (fin != null && assujettissement.dateFin != fin) {
				assujettissement.dateFin = fin;
				assujettissement.motifFin = null;
			}
			return assujettissement;
		}
	}

	@Override
	public abstract String toString();
}
