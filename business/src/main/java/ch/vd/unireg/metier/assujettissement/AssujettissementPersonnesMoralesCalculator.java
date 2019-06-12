package ch.vd.unireg.metier.assujettissement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.MovingWindow;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.metier.common.ForFiscalPrincipalContext;
import ch.vd.unireg.metier.common.Fraction;
import ch.vd.unireg.metier.common.Fractionnements;
import ch.vd.unireg.regimefiscal.RegimeFiscalConsolide;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AssujettissementPersonnesMoralesCalculator implements AssujettissementCalculator<Entreprise> {

	private final TiersService tiersService;
	private final RegimeFiscalService regimeFiscalService;

	public AssujettissementPersonnesMoralesCalculator(TiersService tiersService, RegimeFiscalService regimeFiscalService) {
		this.tiersService = tiersService;
		this.regimeFiscalService = regimeFiscalService;
	}

	/**
	 * Motifs d'ouverture ou de fermeture de for qui dénottent un passage de la frontière suisse
	 */
	private static final Set<MotifFor> MOTIFS_HS = EnumSet.of(MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS);

	/**
	 * Motifs d'ouverture ou de fermeture de for qui dénottent un passage d'une frontière VD &lt;-&gt; HC
	 */
	private static final Set<MotifFor> MOTIFS_HC = EnumSet.of(MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC);

	/**
	 * Collections des motifs d'ouverture de for qui ne donnent normalement pas lieu à un début d'assujettissement
	 * ou qui doivent, le cas échéant, laisser la priorité au motif d'ouverture du for "économique" - si existant à la même date - dans la méthode
	 * {@link #fusionnerMotifs(RegDate, MotifAssujettissement, RegDate, MotifAssujettissement, boolean, Set) fusionnerMotifs}
	 */
	@SuppressWarnings({"deprecation"})
	private static final Set<MotifAssujettissement> MOTIFS_NON_PRIO_OUVERTURE = EnumSet.of(MotifAssujettissement.INDETERMINE,
	                                                                                       MotifAssujettissement.VENTE_IMMOBILIER,
	                                                                                       MotifAssujettissement.ANNULATION,
	                                                                                       MotifAssujettissement.FIN_ACTIVITE_DIPLOMATIQUE,
	                                                                                       MotifAssujettissement.FIN_EXPLOITATION);

	/**
	 * Collections des motifs de fermeture de for qui ne donnent normalement pas lieu à une fin d'assujettissement
	 * ou qui doivent, le cas échéant, laisser la priorité au motif de fermeture du for "économique" - si existant à la même date - dans la méthode
	 * {@link #fusionnerMotifs(RegDate, MotifAssujettissement, RegDate, MotifAssujettissement, boolean, Set) fusionnerMotifs}
	 */
	@SuppressWarnings({"deprecation"})
	private static final Set<MotifAssujettissement> MOTIFS_NON_PRIO_FERMETURE = EnumSet.of(MotifAssujettissement.INDETERMINE,
	                                                                                       MotifAssujettissement.ACHAT_IMMOBILIER,
	                                                                                       MotifAssujettissement.REACTIVATION,
	                                                                                       MotifAssujettissement.DEBUT_ACTIVITE_DIPLOMATIQUE,
	                                                                                       MotifAssujettissement.DEBUT_EXPLOITATION);

	/**
	 * Détermine les fors fiscaux à prendre en compte pour la répartition sur les communes vaudoises
	 */
	public static final AssujettissementSurCommuneAnalyzer COMMUNE_ANALYZER = assujettissement -> {
		// pour l'assujettissement PM, les fors déterminants sont :
		// - tous les fors secondaires dans la période
		// - le dernier for principal vaudois de la période

		final DecompositionFors fors = assujettissement.getFors();
		final List<ForFiscalRevenuFortune> determinants = new ArrayList<>(1 + fors.secondairesDansLaPeriode.size());
		for (ForFiscalPrincipal ffp : CollectionsUtils.revertedOrder(fors.principauxDansLaPeriode)) {
			if (ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				determinants.add(ffp);
				break;
			}
		}
		for (ForFiscalSecondaire ffs : fors.secondairesDansLaPeriode) {
			if (ffs != null && ffs.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				determinants.add(ffs);
			}
		}
		return determinants;
	};

	/**
	 * Calcul de l'assujettissement d'une personne morale d'après ses fors et ses exercices commerciaux
	 * @param entreprise contribuable PM
	 * @param fpt liste des fors triés et classés de la PM
	 * @param noOfsCommunesVaudoises (optionnelle) liste des numéros OFS des communes vaudoises pour lesquelles on veut spécifiquement calculer l'assujettissement
	 * @return la liste des assujettissemnts calculés, ou <code>null</code> si le contribuable n'est pas assujetti du tout
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement du contribuable.
	 */
	@Override
	public List<Assujettissement> determine(Entreprise entreprise, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		// pas de fors, par d'assujettissement...
		if (fpt.principauxPM.isEmpty()) {
			return null;
		}

		// [SIFISC-16333] Seuls les fors dont le genre d'impôt est "bénéfice-capital" peuvent être générateurs d'assujettissement
		final List<ForFiscalPrincipalPM> forsFiscaux = new ArrayList<>(fpt.principauxPM.size());
		for (ForFiscalPrincipalPM ff : fpt.principauxPM) {
			if (ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL) {
				forsFiscaux.add(ff);
			}
		}
		if (forsFiscaux.isEmpty()) {
			return null;
		}

		// filtrage des fors secondaires sur le même principe (= le genre impôt doit être "bénéfice-capital"
		final List<ForFiscalSecondaire> forsSecondaires = new ArrayList<>(fpt.secondaires.size());
		for (ForFiscalSecondaire ffs : fpt.secondaires) {
			if (ffs.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL) {
				forsSecondaires.add(ffs);
			}
		}

		// première chose, recalculer les exercices commerciaux de l'entreprise jusque et y compris l'exercice courant
		final List<ExerciceCommercial> exercices = tiersService.getExercicesCommerciaux(entreprise);

		// les fractionnements de principe de l'assujettissement (départs/arrivées HS, créations, dissolutions...)
		final Fractionnements<ForFiscalPrincipalPM> fractionnements = new FractionnementsAssujettissementPM(fpt.principauxPM);

		// on s'intéresse aux influences des sièges
		final List<Data> siegesBruts = determinerAssujettissementSieges(fpt.principauxPM, fractionnements, exercices, noOfsCommunesVaudoises);
		final List<Data> sieges = compacterNonAssujettissements(siegesBruts, noOfsCommunesVaudoises != null);
		AssujettissementHelper.assertCoherenceRanges(sieges);

		// et finalement aux influences des rattachements économiques
		final List<Data> economiques = determinerAssujettissementEconomique(entreprise, forsSecondaires, fractionnements, exercices, noOfsCommunesVaudoises);

		// fusion des deux
		final List<Data> fusion = fusionnerAssujettissementsSiegesEtEconomiques(sieges, economiques);

		// Appliquer les exonérations totales
		final List<RegimeFiscalConsolide> regimesFiscaux = regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
		final List<Data> fusionAdaptee = appliquerExonerations(fusion, exercices, regimesFiscaux);

		// et transcription en assujettissements officiels
		final List<Assujettissement> result = DateRangeHelper.collate(instanciate(entreprise, fusionAdaptee));
		return result.isEmpty() ? null : result;
	}

	/**
	 * Appliquer les exonérations, c'est à dire retirer de l'assujettissement les plages correspondant aux exercices exonérés, c'est à dire les exercices
	 * qui pour leur dernier jour sont porteurs d'un régime fiscal VD présentant une exonération totale IBC pour la période.
	 *
	 * @param data les plages d'assujettissement à ajuster
	 * @param exercices les exercices commerciaux
	 * @param regimesFiscauxVDNonAnnulesTrie les régimes fiscaux
	 * @return les plages d'assujettissement épurées des périodes d'exonération
	 */
	private static List<Data> appliquerExonerations(List<Data> data, List<ExerciceCommercial> exercices, List<RegimeFiscalConsolide> regimesFiscauxVDNonAnnulesTrie) {
		if (data.isEmpty()) {
			return data;
		}

		// Déterminer la liste des exercices à exonérer
		final List<DateRange> periodesAExclure = new ArrayList<>();
		RegDate dateDebutExerciceInfini = data.get(0).getDateDebut();
		for (final ExerciceCommercial exe : exercices) {
			final RegimeFiscalConsolide regimeFiscalConsolide = DateRangeHelper.rangeAt(regimesFiscauxVDNonAnnulesTrie, exe.getDateFin());
			if (regimeFiscalConsolide != null && (regimeFiscalConsolide.isIndetermine() || isExonerationTotaleIBC(regimeFiscalConsolide, exe.getDateFin().year()))) {
				periodesAExclure.add(exe);
			}
			dateDebutExerciceInfini = exe.getDateFin().getOneDayAfter();
		}
		final RegimeFiscalConsolide regimeFiscalConsolide = DateRangeHelper.rangeAt(regimesFiscauxVDNonAnnulesTrie, dateDebutExerciceInfini);
		if (regimeFiscalConsolide != null && (regimeFiscalConsolide.isIndetermine() || isExonerationTotaleIBC(regimeFiscalConsolide, dateDebutExerciceInfini.year()))) {
			periodesAExclure.add(new DateRangeHelper.Range(dateDebutExerciceInfini, null));
		}

		// Masquer les ranges d'assujettissement d'après la liste
		return DateRangeHelper.subtract(data, periodesAExclure, (range, debut, fin) -> new Data(debut != null ? debut : range.getDateDebut(),
                                                                                        fin != null ? fin : range.getDateFin(),
                                                                                        range.type,
                                                                                        debut != null && debut != range.getDateDebut() ? MotifAssujettissement.EXONERATION : range.motifDebut,
                                                                                        fin != null && fin != range.getDateFin() ? MotifAssujettissement.EXONERATION : range.motifFin,
                                                                                        range.typeAutoriteFiscale));
	}

	private static boolean isExonerationTotaleIBC(RegimeFiscalConsolide rf, int periode) {
		final PlageExonerationFiscale exoneration = rf.getExonerationIBC(periode);
		return exoneration != null && exoneration.getMode() == ModeExoneration.TOTALE;
	}

	/**
	 * @param before un for principal
	 * @param after le for principal suivant
	 * @return <code>true</code> si la transition entre ces deux fors correspond à un passage de la frontière suisse
	 */
	public static boolean isDepartOuArriveeHorsSuisse(ForFiscalPrincipalPM before, ForFiscalPrincipalPM after) {
		// erreur si aucun for
		if (before == null && after == null) {
			throw new IllegalArgumentException("L'un des deux fors au moins doit être non-nul, non ?");
		}

		// si les deux fors sont présents, on compare les autorités fiscales (normalement correctes puisque calculées par la migration
		// puis gérées correctement, mais bon, on n'est que rarement trop prudent...)
		if (before != null && after != null && before.getDateFin() == after.getDateDebut().getOneDayBefore()) {

			// si l'un des deux fors est sur un pays étranger, mais pas les deux... passage HS -> CH ou CH -> HS
			return (before.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS || after.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS)
					&& before.getTypeAutoriteFiscale() != after.getTypeAutoriteFiscale();
		}

		// l'un des deux est nul, on est donc soit avant un trou, soit juste après, on n'a que le motif pour nous aider...
		return (before != null && MOTIFS_HS.contains(before.getMotifFermeture())) || (after != null && MOTIFS_HS.contains(after.getMotifOuverture()));
	}

	/**
	 * @param before un for principal
	 * @param after le for principal suivant
	 * @return <code>true</code> si la transition entre ces deux fors correspond à un passage de frontière inter-cantonale
	 */
	public static boolean isDepartOuArriveeHorsCanton(ForFiscalPrincipalPM before, ForFiscalPrincipalPM after) {
		// erreur si aucun for
		if (before == null && after == null) {
			throw new IllegalArgumentException("L'un des deux fors au moins doit être non-nul, non ?");
		}

		// si les deux fors sont présents, on compare les autorités fiscales (normalement correctes puisque calculées par la migration
		// puis gérées correctement, mais bon, on n'est que rarement trop prudent...)
		if (before != null && after != null && before.getDateFin() == after.getDateDebut().getOneDayBefore()) {

			// si l'un des deux fors est vaudois et l'autre hors-canton : passage HC -> VD ou VD -> HC
			return (before.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && after.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC)
					|| (before.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC && after.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		}

		// l'un des deux est nul, on est donc soit avant un trou, soit juste après, on n'a que le motif pour nous aider...
		return (before != null && MOTIFS_HC.contains(before.getMotifFermeture())) || (after != null && MOTIFS_HC.contains(after.getMotifOuverture()));
	}

	/**
	 * Les différents types d'assujettissements PM
	 */
	private enum Type {
		Vaudois,
		HorsCanton,
		HorsSuisse,
		NonAssujetti
	}

	/**
	 * Données collectées sur un assujettissement PM
	 */
	private static final class Data implements CollatableDateRange<Data> {

		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final Type type;
		private final MotifAssujettissement motifDebut;
		private final MotifAssujettissement motifFin;
		private final TypeAutoriteFiscale typeAutoriteFiscale;

		public Data(RegDate dateDebut, RegDate dateFin, Type type, MotifAssujettissement motifDebut, MotifAssujettissement motifFin, TypeAutoriteFiscale typeAutoriteFiscale) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.type = type;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
		}

		public Data(RegDate dateDebut, RegDate dateFin, Type type, MotifFor motifDebut, MotifFor motifFin, TypeAutoriteFiscale typeAutoriteFiscale) {
			this(dateDebut, dateFin, type, MotifAssujettissement.of(motifDebut), MotifAssujettissement.of(motifFin), typeAutoriteFiscale);
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}

		@Override
		public boolean isCollatable(Data next) {
			return DateRangeHelper.isCollatable(this, next) && motifFin == next.motifDebut && type == next.type && typeAutoriteFiscale == next.typeAutoriteFiscale;
		}

		@NotNull
		@Override
		public Data collate(Data next) {
			return new Data(getDateDebut(), next.getDateFin(), type, motifDebut, next.motifFin, typeAutoriteFiscale);
		}
	}

	/**
	 * @param forsPrincipaux les fors principaux triés de la personne morale
	 * @param fractionnements les fractionnements d'assujettissement calculés pour ces fors
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @param noOfsCommunesVaudoises (optionnel) les numéros OFS des communes pour lesquelles on veut calculer l'assujettissement spécifiquement
	 * @return la liste des données d'assujettissement pour ce qui concerne les sièges
	 * @throws AssujettissementException en cas de problème
	 */
	@NotNull
	private static List<Data> determinerAssujettissementSieges(List<ForFiscalPrincipalPM> forsPrincipaux,
	                                                           Fractionnements<ForFiscalPrincipalPM> fractionnements,
	                                                           List<ExerciceCommercial> exercicesCommerciaux,
	                                                           @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		final List<Data> liste = new LinkedList<>();

		final MovingWindow<ForFiscalPrincipalPM> iter = new MovingWindow<>(forsPrincipaux);
		while (iter.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipalPM> snap = iter.next();

			// on veut la vue des fors qui se touchent tout autour du for principal courant
			final ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal = new ForFiscalPrincipalContext<>(snap);

			// détermination de l'assujettissement généré par le for principal courant
			final Data initial = determiner(forPrincipal, fractionnements, exercicesCommerciaux, noOfsCommunesVaudoises);
			if (initial == null) {
				continue;
			}

			// on fractionne si nécessaire
			final Data apresFractionnement = fractionner(initial, forPrincipal.getCurrent(), fractionnements);
			if (apresFractionnement == null) {
				continue;
			}

			// en voilà un !
			liste.add(apresFractionnement);
		}

		return liste;
	}

	/**
	 * Détermination de l'assujettissement induit par un for principal
	 * @param forPrincipal le for principal en question
	 * @param fractionnements les fractionnements préalablement calculés
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @param noOfsCommunesVaudoises (optionnel) les numéros OFS des communes pour lesquelles on veut calculer l'assujettissement spécifiquement
	 * @return l'assujettissement induit par le for principal
	 * @throws AssujettissementException en cas de problème
	 */
	@Nullable
	private static Data determiner(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal,
	                               Fractionnements<ForFiscalPrincipalPM> fractionnements,
                                   List<ExerciceCommercial> exercicesCommerciaux,
                                   @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final Data data;
		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();

		// [SIFISC-16333] Seuls les fors principaux avec un genre d'impôt "bénéfice/capital" donnent lieu à un assujettissement
		if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && current.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL) {

			final RegDate debut = determinerDateDebutAssujettissement(forPrincipal, fractionnements, exercicesCommerciaux);
			final RegDate fin = determinerDateFinAssujettissement(forPrincipal, fractionnements, exercicesCommerciaux);
			if (RegDateHelper.isBeforeOrEqual(debut, fin, NullDateBehavior.LATEST)) {

				// si on demande une limitation sur certaines communes vaudoises et que le for n'y est pas, il s'agit d'un "non-assujettissement"
				if (noOfsCommunesVaudoises != null && !noOfsCommunesVaudoises.contains(current.getNumeroOfsAutoriteFiscale())) {
					data = new Data(debut, fin, Type.NonAssujetti, (MotifAssujettissement) null, null, current.getTypeAutoriteFiscale());
				}
				else {
					// on vérifie qu'il s'agit bien d'un motif de rattachement DOMICILE (TODO SIEGE ?)
					if (current.getMotifRattachement() != MotifRattachement.DOMICILE) {
						throw new AssujettissementException(String.format("Le contribuable %s possède un for principal avec un motif de rattachement [%s], ce qui est interdit.",
						                                                  FormatNumeroHelper.numeroCTBToDisplay(current.getTiers().getNumero()),
						                                                  current.getMotifRattachement()));
					}

					// le cas général, assujettissement
					data = new Data(debut, fin, Type.Vaudois, current.getMotifOuverture(), current.getMotifFermeture(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				}
			}
			else {
				// pas de période réelle d'assujettissement
				data = null;
			}
		}
		else {
			final RegDate adebut = determinerDateDebutNonAssujettissement(forPrincipal, exercicesCommerciaux);
			final RegDate afin = determinerDateFinNonAssujettissement(forPrincipal, exercicesCommerciaux);

			if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
				data = new Data(adebut, afin, Type.NonAssujetti, current.getMotifOuverture(), current.getMotifFermeture(), current.getTypeAutoriteFiscale());
			}
			else {
				// pas de période réelle d'assujettissement
				data = null;
			}
		}

		return data;
	}

	/**
	 * @param exercicesCommerciaux la liste des exercices commerciaux d'une personne morale
	 * @param reference une date de référence
	 * @return l'exercice commercial valide à la date donnée
	 * @throws AssujettissementException en cas de problème
	 */
	@NotNull
	private static ExerciceCommercial getExerciceCommercialAt(List<ExerciceCommercial> exercicesCommerciaux, @NotNull RegDate reference) throws AssujettissementException {
		final ExerciceCommercial exercice = DateRangeHelper.rangeAt(exercicesCommerciaux, reference);
		if (exercice == null) {
			throw new AssujettissementException(String.format("Pas d'exercice commercial valide au %s.", RegDateHelper.dateToDisplayString(reference)));
		}
		return exercice;
	}

	/**
	 * @param exercicesCommerciaux la liste des exercices commerciaux d'une personne morale
	 * @param reference une date de référence
	 * @return la date de début de l'exercice commercial en cours à la date de référence
	 * @throws AssujettissementException en cas de problème
	 */
	@NotNull
	private static RegDate getDernierDebutExercice(List<ExerciceCommercial> exercicesCommerciaux, @NotNull RegDate reference) throws AssujettissementException {
		return getExerciceCommercialAt(exercicesCommerciaux, reference).getDateDebut();
	}

	/**
	 * @param exercicesCommerciaux la liste des exercices commerciaux d'une personne morale
	 * @param reference une date de référence
	 * @return la date de fin de l'exercice commercial en cours à la date de référence
	 * @throws AssujettissementException en cas de problème
	 */
	@NotNull
	private static RegDate getProchaineFinExercice(List<ExerciceCommercial> exercicesCommerciaux, @NotNull RegDate reference) throws AssujettissementException {
		return getExerciceCommercialAt(exercicesCommerciaux, reference).getDateFin();
	}

	/**
	 * Détermination de la date de début de l'assujettissement induit par un for principal
	 * @param forPrincipal le for principal en question
	 * @param fractionnements les fractionnements préalablement calculés
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @return la date de début de l'assujettissement induit par le for principal
	 * @throws AssujettissementException en cas de problème
	 */
	@NotNull
	private static RegDate determinerDateDebutAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal,
	                                                           Fractionnements<ForFiscalPrincipalPM> fractionnements,
	                                                           List<ExerciceCommercial> exercicesCommerciaux) throws AssujettissementException {


		final RegDate debut;
		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();
		final Fraction fraction = fractionnements.getAt(current.getDateDebut());

		if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			debut = current.getDateDebut();
		}
		else if (fraction != null) {
			debut = fraction.getDate();
		}
		else {
			// dans tous les autres cas, l'assujettissement débute au début de l'exercice commercial
			debut = getDernierDebutExercice(exercicesCommerciaux, current.getDateDebut());
		}

		return debut;
	}

	/**
	 * Détermination de la date de fin de l'assujettissement induit par un for principal
	 * @param forPrincipal le for principal en question
	 * @param fractionnements les fractionnements préalablement calculés
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @return la date de fin de l'assujettissement induit par le for principal
	 * @throws AssujettissementException en cas de problème
	 */
	@Nullable
	private static RegDate determinerDateFinAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal,
	                                                         Fractionnements<ForFiscalPrincipalPM> fractionnements,
	                                                         List<ExerciceCommercial> exercicesCommerciaux) throws AssujettissementException {


		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();
		final RegDate fin = current.getDateFin();

		final Fraction fraction = (fin == null ? null : fractionnements.getAt(fin.getOneDayAfter()));

		final RegDate afin;
		if (fin == null || current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			afin = fin;
		}
		else if (fraction != null) {
			afin = fraction.getDate().getOneDayBefore();
		}
		else if (forPrincipal.getNext() != null && !isDepartOuArriveeHorsCanton(current, forPrincipal.getNext()) && !isDepartOuArriveeHorsSuisse(current, forPrincipal.getNext()) && !isSurBouclement(fin, exercicesCommerciaux)) {
			// si on change de commune, alors l'assujettissement sera généré par le for suivant...
			// [SIFISC-21921] (sauf bien-sûr si le départ se produit le dernier jour de l'exercice...)
			afin = getDernierDebutExercice(exercicesCommerciaux, fin).getOneDayBefore();
		}
		else {
			// dans tous les autres cas, l'assujettissement finit à la fin de l'exercice commercial
			afin = getProchaineFinExercice(exercicesCommerciaux, fin);
		}

		return afin;
	}

	/**
	 * @param date date à tester par rapport aux dates de bouclement connues
	 * @param exercicesCommerciaux les exercices commerciaux de l'entreprise
	 * @return <code>true</code> si la date de fin du for fiscal correspond à une date de fin d'exercice commercial
	 */
	private static boolean isSurBouclement(@NotNull RegDate date, List<ExerciceCommercial> exercicesCommerciaux) {
		final ExerciceCommercial exerciceCourant = DateRangeHelper.rangeAt(exercicesCommerciaux, date);
		return exerciceCourant != null && exerciceCourant.getDateFin() == date;
	}

	/**
	 * Détermination de la date de début du non-assujettissement induit par un for principal hors-canton ou hors-Suisse
	 * @param forPrincipal le for principal en question
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @return la date de début du non-assujettissement induit par le for principal
	 * @throws AssujettissementException en cas de problème
	 */
	private static RegDate determinerDateDebutNonAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal,
	                                                              List<ExerciceCommercial> exercicesCommerciaux) throws AssujettissementException {

		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();
		final RegDate debut = current.getDateDebut();
		final RegDate adebut;

		// Non-assujettissement -> le for principal est HC ou HS (ou vaudois si le genre d'impôt du for n'est pas le bon)
		if (current.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS) {
			// le rattachement économique se termine à la fin de l'exercice commercial
			adebut = getDernierDebutExercice(exercicesCommerciaux, debut);
		}
		else { // for hors-Suisse
			adebut = debut; // le rattachement économmique est limité à la période de validité du for pour les HS
		}

		return adebut;

	}

	/**
	 * Détermination de la date de fin du non-assujettissement induit par un for principal hors-canton ou hors-Suisse
	 * @param forPrincipal le for principal en question
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @return la date de fin du non-assujettissement induit par le for principal
	 * @throws AssujettissementException en cas de problème
	 */
	private static RegDate determinerDateFinNonAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPM> forPrincipal,
	                                                            List<ExerciceCommercial> exercicesCommerciaux) throws AssujettissementException {

		final ForFiscalPrincipalPM current = forPrincipal.getCurrent();
		final RegDate fin = current.getDateFin();

		final RegDate afin;
		if (fin == null) {
			afin = null;
		}

		// Non-assujettissement -> le for principal est HC ou HS (ou vaudois si le genre d'impôt du for n'est pas le bon)
		else if (current.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS) {
			// le rattachement économique commence au début de l'exercice commercial
			afin = getProchaineFinExercice(exercicesCommerciaux, fin);
		}
		else {
			afin = fin; // le rattachement économmique est limité à la période de validité du for pour les HS
		}

		return afin;
	}

	/**
	 * Application des règles de fractionnement sur les données d'assujettissement fournies
	 * @param data les fameuses données d'assujettissement
	 * @param forPrincipal le for principal source de l'assujettissement
	 * @param fractionnements les données précalculées du fractionnement
	 * @return la donnée d'assujettissement valide après application des règles de fractionnement
	 * @throws AssujettissementException en cas de problème
	 */
	private static Data fractionner(Data data, ForFiscalPrincipalPM forPrincipal, Fractionnements<ForFiscalPrincipalPM> fractionnements) throws AssujettissementException {

		// si pas de fractionnement en général, pas besoin d'en faire plus
		if (fractionnements.isEmpty()) {
			return data;
		}

		// on détermine les fractionnements immédiatement à gauche et droite du for principal à la source
		// de l'assujettissement (logiquement, il n'est pas possible d'avoir un fractionnement à l'intérieur du for)
		final LimitesAssujettissement limites = LimitesAssujettissement.determine(forPrincipal, fractionnements);
		final Fraction left = (limites == null ? null : limites.getLeft());
		final Fraction right = (limites == null ? null : limites.getRight());

		// on prépare les données pour une version rognée...
		RegDate debut = data.getDateDebut();
		RegDate fin = data.getDateFin();
		MotifAssujettissement motifDebut = data.motifDebut;
		MotifAssujettissement motifFin = data.motifFin;

		// on réduit l'assujettissement en conséquence

		if (left != null && left.getDate().isAfter(debut)) {
			debut = left.getDate();
			motifDebut = MotifAssujettissement.of(left.getMotif());
		}

		if (right != null && right.getDate().isBeforeOrEqual(fin)) {
			fin = right.getDate().getOneDayBefore();
			motifFin = MotifAssujettissement.of(right.getMotif());
		}

		return new Data(debut, fin, data.type, motifDebut, motifFin, data.typeAutoriteFiscale);
	}

	/**
	 * Compacte les non-assujettissements et laisse la place aux assujettissements positifs en cas de conflit
	 * avec des non-assujettissements
	 * @param data les données d'assujettissement calculées jusque là
	 * @param forRolesCommunes <code>true</code> si nous sommes dans le contexte des rôles des communes (assujettissements différenciés par commune...)
	 * @return une liste contenant les données d'assujettissement avec les non-assujettissement correctement compactés
	 * @throws AssujettissementException en cas de problème
	 */
	private List<Data> compacterNonAssujettissements(List<Data> data, boolean forRolesCommunes) throws AssujettissementException {

		// on sépare d'abord les non-assujettissements des autres
		final List<Data> nonAssujettissements = new ArrayList<>(data.size());
		final List<Data> assujettissementsPositifs = new ArrayList<>(data.size());
		for (Data a : data) {
			if (a.type == Type.NonAssujetti) {
				nonAssujettissements.add(a);
			}
			else {
				assujettissementsPositifs.add(a);
			}
		}

		// fusion des assujettissements
		final List<Data> assujettissementsFusionnes = fusionnerAssujettissements(assujettissementsPositifs);

		// fusion des non-assujettissements
		final List<Data> nonAssujettissementsFusionnes = fusionnerNonAssujettissements(nonAssujettissements, forRolesCommunes);

		// on réduit la durée des non-assujettissement si nécessaire
		return DateRangeHelper.override(nonAssujettissementsFusionnes, assujettissementsFusionnes, new DateRangeHelper.AdapterCallbackExtended<Data>() {
			@Override
			public Data adapt(Data range, RegDate debut, RegDate fin) {
				throw new IllegalArgumentException("Ne devrait pas être appelé");
			}

			@Override
			public Data adapt(Data range, RegDate debut, Data surchargeDebut, RegDate fin, Data surchargeFin) {
				return new Data(debut != null ? debut : range.getDateDebut(),
				                fin != null ? fin : range.getDateFin(),
				                range.type,
				                debut != null ? surchargeDebut.motifFin : range.motifDebut,
				                fin != null ? surchargeFin.motifDebut : range.motifFin,
				                range.typeAutoriteFiscale);
			}

			@Override
			public Data duplicate(Data range) {
				// pas besoin de dupliquer l'objet car il est immutable
				return range;
			}
		});
	}

	/**
	 * Fusionne les assujettissements qui s'intersectent. Des assujettissements peuvent s'intersecter lorsque - par exemple - un contribuable
	 * possède plusieurs fors fiscaux principaux vaudois dans un même exercice commercial (= déménagement)
	 *
	 * @param list  une liste de données qui doivent être des assujettisssments "positifs"
	 * @return la liste fusionnée des assujettissments
	 */
	private List<Data> fusionnerAssujettissements(List<Data> list) {
		if (list.isEmpty()) {
			return list;
		}

		final List<Data> merged = DateRangeHelper.merge(list, DateRangeHelper.MergeMode.INTERSECTING, new DateRangeHelper.MergeCallback<Data>() {
			@Override
			public Data merge(Data left, Data right) {
				if (left.type != right.type) {
					throw new IllegalArgumentException("Deux données d'assujettissements qui s'intersectent mais de types différents (" + left.type + " et " + right.type + ") !");
				}
				if (left.typeAutoriteFiscale != right.typeAutoriteFiscale) {
					throw new IllegalArgumentException(
							"Deux données d'assujettissements qui s'intersectent avec des autorités fiscales différentes (" + left.typeAutoriteFiscale + " et " + right.typeAutoriteFiscale + ") !");
				}

				return new Data(left.getDateDebut(),
				                right.getDateFin(),
				                left.type,
				                left.motifDebut,
				                right.motifFin,
				                left.typeAutoriteFiscale);
			}

			@Override
			public Data duplicate(Data range) {
				// pas la peine de dupliquer quoi que ce soit, cette classe est immutable!
				return range;
			}
		});

		return DateRangeHelper.collate(merged);
	}

	/**
	 * Fusionne les non-assujettissements qui s'intersectent. Des non-assujettissements peuvent s'intersecter lorsque - par exemple - un contribuable
	 * possède plusieurs fors fiscaux principaux hors-canton dans une même année.
	 *
	 * @param list             une liste de données qui doivent être des non-assujettisssments
	 * @param forRolesCommunes <b>vrai</b> si cette méthode est appelée dans le contexte du rôle des communes; <b>faux</b> autrement.
	 * @return la liste fusionnée des non-assujettissments
	 */
	private List<Data> fusionnerNonAssujettissements(List<Data> list, final boolean forRolesCommunes) {
		if (list.isEmpty()) {
			return list;
		}

		final List<Data> merged = DateRangeHelper.merge(list, DateRangeHelper.MergeMode.INTERSECTING, new DateRangeHelper.MergeCallback<Data>() {
			@Override
			public Data merge(Data left, Data right) {
				final RegDate debut = RegDateHelper.minimum(left.getDateDebut(), right.getDateDebut(), NullDateBehavior.EARLIEST);
				final RegDate fin = RegDateHelper.maximum(left.getDateFin(), right.getDateFin(), NullDateBehavior.LATEST);
				final TypeAutoriteFiscale typeAut = fusionnerTypeAutPourNonAssujettissements(left, right, forRolesCommunes);
				return new Data(debut, fin, Type.NonAssujetti, left.motifDebut, right.motifFin, typeAut);
			}

			@Override
			public Data duplicate(Data range) {
				// pas besoin de dupliquer l'objet, puisqu'il est immutable
				return range;
			}
		});
		return DateRangeHelper.collate(merged);
	}

	private static TypeAutoriteFiscale fusionnerTypeAutPourNonAssujettissements(Data left, Data right, boolean forRolesCommunes) {
		if (forRolesCommunes) {
			// [SIFISC-4682] Dans le cas du calcul de l'assujettissement du point de vue d'une commune vaudoise, il peut y avoir
			// un for fiscal principal hors-canton associé à un for secondaire dans le canton qui provoquent chacun un non-assujettissement
			// de type différent (hors-canton et commune_vd) et qui se chevauchent.
			// Il s'agit d'une situation est correcte dans ce cas-là, et le type d'autorité fiscale qui nous intéresse est celle qui n'est PAS vaudoise
			// (puisque si un for fiscal vaudois a généré un non-assujettissement, c'est justement parce qu'on ne veut pas en tenir compte).
			return left.typeAutoriteFiscale != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD ? left.typeAutoriteFiscale : right.typeAutoriteFiscale;
		}
		else {
			// Dans le cas du calcul de l'assujettissement normal, seuls les fors fiscaux principaux hors-canton et hors-Suisse peuvent
			// générer des non-assujettissement. Comme il y a forcément un fractionnement entre un for hors-Suisse et un for d'un autre type,
			// on ne devrait jamais avoir des fors avec des types différents qui se chevauchent, sauf en cas d'erreur dans l'algorithme.
			if (left.typeAutoriteFiscale != right.typeAutoriteFiscale) {
				throw new IllegalArgumentException("Détecté deux non-assujettissements de type différents qui s'intersectent [" + left + "] et [" + right + "] (erreur dans l'algorithme ?)");
			}
			return left.typeAutoriteFiscale;
		}
	}

	/**
	 * Calcul de l'assujettissement économique dû aux fors secondaires
	 * @param entreprise l'entreprise
	 * @param forsSecondaires les fors secondaires triés
	 * @param fractionnements les fractionnements précalculés
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @param noOfsCommunesVaudoises (optionnel) les numéros OFS des communes pour lesquelles on veut calculer spécifiquement l'assujettissement (= rôles)
	 * @return la liste des données d'assujettissements économiques
	 * @throws AssujettissementException en cas de problème
	 */
	private static List<Data> determinerAssujettissementEconomique(Entreprise entreprise,
	                                                               List<ForFiscalSecondaire> forsSecondaires,
	                                                               Fractionnements<ForFiscalPrincipalPM> fractionnements,
	                                                               List<ExerciceCommercial> exercicesCommerciaux,
	                                                               @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final List<Data> data = new ArrayList<>(forsSecondaires.size());
		for (ForFiscalSecondaire ffs : forsSecondaires) {
			final Data economique = determinerAssujettissementEconomique(entreprise, ffs, fractionnements, exercicesCommerciaux, noOfsCommunesVaudoises);
			if (economique != null) {
				data.add(economique);
			}
		}
		return data;
	}

	/**
	 * Calcul de l'assujettissement économique dû à un for secondaire
	 * @param entreprise l'entreprise
	 * @param forSecondaire le for secondaire analysé
	 * @param fractionnements les fractionnements précalculés
	 * @param exercicesCommerciaux les exercices commerciaux de la personne morale
	 * @param noOfsCommunesVaudoises (optionnel) les numéros OFS des communes pour lesquelles on veut calculer spécifiquement l'assujettissement (= rôles)
	 * @return la liste des données d'assujettissements économiques
	 * @throws AssujettissementException en cas de problème
	 */
	@Nullable
	private static Data determinerAssujettissementEconomique(Entreprise entreprise,
	                                                         ForFiscalSecondaire forSecondaire,
	                                                         Fractionnements<ForFiscalPrincipalPM> fractionnements,
	                                                         List<ExerciceCommercial> exercicesCommerciaux,
	                                                         @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		// si le for secondaire n'est pas sur une des communes explicitement demandées, il n'y a pas d'assujettissement
		if (noOfsCommunesVaudoises != null && !noOfsCommunesVaudoises.contains(forSecondaire.getNumeroOfsAutoriteFiscale())) {
			return null;
		}

		// si le for principal est Hors-Suisse au moment du début du for secondaire, l'assujettissement économique commence
		// à la date de début du for, mais sinon, il commence au début de l'exercice commercial actif à l'ouverture du for
		final RegDate dateDebutFor = forSecondaire.getDateDebut();
		RegDate dateDebutAssujettissement;
		if (AssujettissementHelper.isForPrincipalHorsSuisse(entreprise, dateDebutFor)) {
			dateDebutAssujettissement = dateDebutFor;
		}
		else {
			dateDebutAssujettissement = getDernierDebutExercice(exercicesCommerciaux, dateDebutFor);
		}

		// si le for n'est pas terminé, l'assujettissement non plus
		// si le for principal est hors-Suisse au moment de la fin du for secondaire, l'assujettissement économique s'arrête à cette date,
		// mais il se poursuit jusqu'à la fin de l'exercice commercial en cours sinon
		final RegDate dateFinFor = forSecondaire.getDateFin();
		RegDate dateFinAssujettissement;
		if (dateFinFor == null || AssujettissementHelper.isForPrincipalHorsSuisse(entreprise, dateFinFor)) {
			dateFinAssujettissement = dateFinFor;
		}
		else {
			dateFinAssujettissement = getProchaineFinExercice(exercicesCommerciaux, dateFinFor);
		}

		// Dans tous les cas, si on trouve une date de fractionnement entre la date réelle du début du for et la date de début de l'assujettissement,
		// on adapte cette dernière en conséquence. Même chose pour les dates de fin d'assujettissement.
		for (Fraction f : fractionnements) {
			if (RegDateHelper.isBetween(f.getDate(), dateDebutAssujettissement, dateDebutFor, NullDateBehavior.LATEST)) {
				dateDebutAssujettissement = f.getDate();
			}

			if (dateFinFor != null && RegDateHelper.isBetween(f.getDate().getOneDayBefore(), dateFinFor, dateFinAssujettissement, NullDateBehavior.LATEST)) {
				dateFinAssujettissement = f.getDate().getOneDayBefore();
			}
		}

		// dates cohérentes après correction ?
		if (RegDateHelper.isBeforeOrEqual(dateDebutAssujettissement, dateFinAssujettissement, NullDateBehavior.LATEST)) { // [UNIREG-2559]
			return new Data(dateDebutAssujettissement,
			                dateFinAssujettissement,
			                null,
			                forSecondaire.getMotifOuverture(),
			                forSecondaire.getMotifFermeture(),
			                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		}

		// pas de période viable d'assujettissement trouvée
		return null;
	}

	/**
	 * Fusion des assujettissements sièges et économiques dans une seule collection
	 * @param sieges données d'assujettissements "sièges"
	 * @param economiques données d'assujettissements "économiques"
	 * @return une liste des données d'assujettissement fusionnées
	 * @throws AssujettissementException en cas de problème
	 */
	@NotNull
	private static List<Data> fusionnerAssujettissementsSiegesEtEconomiques(List<Data> sieges, List<Data> economiques) throws AssujettissementException {

		final List<Data> fusion = new ArrayList<>();        // aucune idée sur la taille raisonnable à mettre ici, mais comme on a des accès par indexe plus bas, je garde ArrayList...
		fusion.addAll(sieges);

		for (int i = 0; i < fusion.size(); i++) {
			for (Data e : economiques) {
				final Data d = fusion.get(i);
				final List<Data> sub = fusionnerAssujettissementsSiegeEtEconomique(d, e);
				if (sub != null) {
					fusion.set(i, sub.get(0));
					fusion.addAll(i + 1, sub.subList(1, sub.size()));
				}
			}
		}

		fusion.sort(new DateRangeComparator<>());
		return fusion;
	}

	/**
	 * @param assujettissement assujettissement (y compris non-assujettissement) issu des sièges ou de fusions préalables de sièges avec des données économiques
	 * @param economique nouvelle donnée économique à prendre en compte
	 * @return liste des données résultantes de la fusion (<code>null</code> si l'assujettissement source n'est pas impacté, et non-<code>null</code> si la fusion est effective)
	 */
	@Nullable
	private static List<Data> fusionnerAssujettissementsSiegeEtEconomique(Data assujettissement, Data economique) {

		// si l'assujettissement de base est assujetti, il a précédence sur tout assujettissement économique
		// donc seul l'assujettissement de base est conservé
		if (assujettissement.type != Type.NonAssujetti) {
			return null;
		}

		// si les deux assujettissements ne s'intersectent pas, il n'y a pas d'influence de l'un sur l'autre
		if (!DateRangeHelper.intersect(assujettissement, economique)) {
			return null;
		}

		// doit-il y avoir un découpage ?
		// - si l'assujettissement économique couvre complètement le non-assujettissement, il n'y aura qu'un morceau
		// - sinon, il y a au moins une période (peut-être deux, de chaque côté) qui restera "non-assujettie" -> il faudra plusieurs morceaux (2 ou 3 selon les cas)
		final List<Data> liste = new ArrayList<>(3);

		// premier morceau = la période entre le début du non-assujettissement et le début de l'assujettissement économique (en non-assujetti)
		if (RegDateHelper.isAfter(economique.getDateDebut(), assujettissement.getDateDebut(), NullDateBehavior.EARLIEST)) {
			liste.add(new Data(assujettissement.getDateDebut(),
			                   economique.getDateDebut().getOneDayBefore(),
			                   assujettissement.type,
			                   assujettissement.motifDebut,
			                   economique.motifDebut,
			                   assujettissement.typeAutoriteFiscale));
		}

		// deuxième morceau = l'assujettissement économique (en HC ou HS)
		final RegDate dateDebutAssujettissementHorsVaud = RegDateHelper.maximum(assujettissement.getDateDebut(), economique.getDateDebut(), NullDateBehavior.EARLIEST);
		final RegDate dateFinAssujettissementHorsVaud = RegDateHelper.minimum(assujettissement.getDateFin(), economique.getDateFin(), NullDateBehavior.LATEST);
		liste.add(new Data(dateDebutAssujettissementHorsVaud,
		                   dateFinAssujettissementHorsVaud,
		                   fromTypeAutoriteFiscale(assujettissement.typeAutoriteFiscale),
		                   fusionnerMotifs(assujettissement.getDateDebut(), assujettissement.motifDebut, economique.getDateDebut(), economique.motifDebut, dateDebutAssujettissementHorsVaud == assujettissement.getDateDebut(), MOTIFS_NON_PRIO_OUVERTURE),
		                   fusionnerMotifs(assujettissement.getDateFin(), assujettissement.motifFin, economique.getDateFin(), economique.motifFin, dateFinAssujettissementHorsVaud == assujettissement.getDateFin(), MOTIFS_NON_PRIO_FERMETURE),
		                   assujettissement.typeAutoriteFiscale));

		// troisième morceau potentiel = la période entre la fin de l'assujettissement économique et la fin du non-assujettissement (en non-assujetti)
		if (RegDateHelper.isAfter(assujettissement.getDateFin(), economique.getDateFin(), NullDateBehavior.LATEST)) {
			liste.add(new Data(economique.getDateFin().getOneDayAfter(),
			                   assujettissement.getDateFin(),
			                   assujettissement.type,
			                   economique.motifFin,
			                   assujettissement.motifFin,
			                   assujettissement.typeAutoriteFiscale));
		}

		return liste;
	}

	/**
	 * @param taf le type d'autorité fiscale du for principal
	 * @return le type d'assujettissement qui découle du for principal en présence d'un for secondaire (= rattachement économique)
	 */
	private static Type fromTypeAutoriteFiscale(TypeAutoriteFiscale taf) {
		switch (taf) {
		case COMMUNE_HC:
			return Type.HorsCanton;
		case PAYS_HS:
			return Type.HorsSuisse;
		case COMMUNE_OU_FRACTION_VD:
			return Type.Vaudois;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnue : " + taf);
		}
	}

	/**
	 * A priori, le motif est celui du siège, sauf s'il est <code>null</code> ou dans la collection des motifs non-prioritaires, que les deux dates sont les mêmes et que le motif écomique existe
	 * @param dateSiege date de référence du motif siège
	 * @param motifSiege motif côté siège
	 * @param dateEconomique date de référence du motif économique
	 * @param motifEconomique motif côté économique
	 * @param dateEffectiveIsSiege <code>true</code> si la date de valeur pour le motif recherché est finalement la date de siège, <code>false</code> si c'est la date économique (important si les dates sont effectivement différentes...)
	 * @param nonPrioritaires ensembles des motifs sièges considérés comme non-prioritaires
	 * @return un motif considéré comme le plus pertinent
	 */
	@Nullable
	private static MotifAssujettissement fusionnerMotifs(@Nullable RegDate dateSiege, @Nullable MotifAssujettissement motifSiege,
	                                                     @Nullable RegDate dateEconomique, @Nullable MotifAssujettissement motifEconomique,
	                                                     boolean dateEffectiveIsSiege,
	                                                     Set<MotifAssujettissement> nonPrioritaires) {

		if (dateSiege == dateEconomique && motifEconomique != null && (motifSiege == null || nonPrioritaires.contains(motifSiege))) {
			return motifEconomique;
		}
		else if (dateEffectiveIsSiege) {
			return motifSiege;
		}
		else {
			return motifEconomique;
		}
	}

	/**
	 * @param ctb le contribuable pour lequel les données d'assujettissement ont été collectées
	 * @param donneesAssujettissement les données d'assujettissement collectées
	 * @return la liste des assujettissements officiels du contribuable
	 * @throws AssujettissementException en cas de problème
	 */
	private static List<Assujettissement> instanciate(ContribuableImpositionPersonnesMorales ctb, List<Data> donneesAssujettissement) throws AssujettissementException {
		final List<Assujettissement> list = new ArrayList<>(donneesAssujettissement.size());
		for (Data d : donneesAssujettissement) {

			final Assujettissement a;
			switch (d.type) {
			case Vaudois:
				a = new VaudoisOrdinaire(ctb, d.getDateDebut(), d.getDateFin(), d.motifDebut, d.motifFin, COMMUNE_ANALYZER);
				break;
			case HorsCanton:
				a = new HorsCanton(ctb, d.getDateDebut(), d.getDateFin(), d.motifDebut, d.motifFin, COMMUNE_ANALYZER);
				break;
			case HorsSuisse:
				a = new HorsSuisse(ctb, d.getDateDebut(), d.getDateFin(), d.motifDebut, d.motifFin, COMMUNE_ANALYZER);
				break;
			case NonAssujetti:
				a = null;
				break;
			default:
				throw new IllegalArgumentException("Type de données d'assujettissement PM inconnu : " + d.type);
			}

			if (a != null) {
				list.add(a);
			}
		}
		return list;
	}
}


