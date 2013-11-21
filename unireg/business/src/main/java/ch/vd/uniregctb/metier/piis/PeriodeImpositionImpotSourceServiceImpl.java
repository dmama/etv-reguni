package ch.vd.uniregctb.metier.piis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class PeriodeImpositionImpotSourceServiceImpl implements PeriodeImpositionImpotSourceService {

	private static final Map<MotifFor, DateShiftingStrategy> BEGIN_DATE_SHIFTING_STRATEGIES_SRC_ORD = buildBeginDateShiftingStrategies(true);
	private static final Map<MotifFor, DateShiftingStrategy> BEGIN_DATE_SHIFTING_STRATEGIES_DEFAULT = buildBeginDateShiftingStrategies(false);
	private static final Map<MotifFor, DateShiftingStrategy> END_DATE_SHIFTING_STRATEGIES_DEFAULT = buildEndDateShiftingStrategies(false);

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private ServiceInfrastructureService infraService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	private static interface DateShiftingStrategy {
		RegDate shift(RegDate date);
		boolean isImperativeSegmentationPoint();
	}

	private static final class NoopDateShiftingStrategy implements DateShiftingStrategy {
		private final boolean imperativeSegmentationPoint;

		public NoopDateShiftingStrategy(boolean imperativeSegmentationPoint) {
			this.imperativeSegmentationPoint = imperativeSegmentationPoint;
		}

		@Override
		public RegDate shift(RegDate date) {
			return date;
		}

		@Override
		public boolean isImperativeSegmentationPoint() {
			return imperativeSegmentationPoint;
		}
	}

	private static abstract class AdditionalTranslationDateShiftingStrategy implements DateShiftingStrategy {
		private final DateShiftingStrategy wrapped;

		public AdditionalTranslationDateShiftingStrategy(DateShiftingStrategy wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public final RegDate shift(RegDate date) {
			return translate(wrapped.shift(date));
		}

		@Override
		public final boolean isImperativeSegmentationPoint() {
			return wrapped.isImperativeSegmentationPoint();
		}

		protected abstract RegDate translate(RegDate date);
	}

	private static final class NextDayDateShiftingStrategy extends AdditionalTranslationDateShiftingStrategy {
		public NextDayDateShiftingStrategy(DateShiftingStrategy wrapped) {
			super(wrapped);
		}

		@Override
		protected RegDate translate(RegDate date) {
			return date.getOneDayAfter();
		}
	}

	private static final class PreviousDayDateShiftingStrategy extends AdditionalTranslationDateShiftingStrategy {
		public PreviousDayDateShiftingStrategy(DateShiftingStrategy wrapped) {
			super(wrapped);
		}

		@Override
		protected RegDate translate(RegDate date) {
			return date.getOneDayBefore();
		}
	}

	private static final class EndOfMonthDateShiftingStrategy implements DateShiftingStrategy {
		@Override
		public RegDate shift(RegDate date) {
			return date.getLastDayOfTheMonth();
		}

		@Override
		public boolean isImperativeSegmentationPoint() {
			return false;
		}
	}

	private static final class NextBeginOfMonthDateShiftingStrategy implements DateShiftingStrategy {
		@Override
		public RegDate shift(RegDate date) {
			return date.getOneDayBefore().getLastDayOfTheMonth().getOneDayAfter();
		}

		@Override
		public boolean isImperativeSegmentationPoint() {
			return false;
		}
	}

	/**
	 * Construction des stratégies de décalage des dates de début
	 */
	private static Map<MotifFor, DateShiftingStrategy> buildBeginDateShiftingStrategies(boolean passageSourceOrdinaire) {
		final Map<MotifFor, DateShiftingStrategy> map = new EnumMap<>(MotifFor.class);

		//
		// les cas de décalage au début du mois suivant
		//
		final DateShiftingStrategy beginOfNextMonth = new NextDayDateShiftingStrategy(new EndOfMonthDateShiftingStrategy());
		if (passageSourceOrdinaire) {
			map.put(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, beginOfNextMonth);
		}
		map.put(MotifFor.ARRIVEE_HC, beginOfNextMonth);
		map.put(MotifFor.DEPART_HC, beginOfNextMonth);

		//
		// le permis C / nationalité est un cas particulier -> on prend le prochain début de mois (on reste sur la date courante si on est déjà en début de mois)
		//
		if (passageSourceOrdinaire) {
			map.put(MotifFor.PERMIS_C_SUISSE, new NextBeginOfMonthDateShiftingStrategy());
		}

		//
		// pas de décalage de date, mais des motifs suffisament impératifs pour contrer un décalage de date, justement
		//
		final NoopDateShiftingStrategy imperativeNoop = new NoopDateShiftingStrategy(true);
		map.put(MotifFor.ARRIVEE_HS, imperativeNoop);
		map.put(MotifFor.DEPART_HS, imperativeNoop);
		map.put(MotifFor.VEUVAGE_DECES, imperativeNoop);

		//
		// tous les autres -> Noop
		//
		final DateShiftingStrategy noop = new NoopDateShiftingStrategy(false);
		for (MotifFor motif : MotifFor.values()) {
			if (!map.containsKey(motif)) {
				map.put(motif, noop);
			}
		}
		return map;
	}

	private static Map<MotifFor, DateShiftingStrategy> buildEndDateShiftingStrategies(boolean passageSourceOrdinaire) {
		final Map<MotifFor, DateShiftingStrategy> map = new EnumMap<>(MotifFor.class);

		//
		// les cas de décalage en fin de mois
		//
		final DateShiftingStrategy endOfMonth = new EndOfMonthDateShiftingStrategy();
		if (passageSourceOrdinaire) {
			map.put(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, endOfMonth);
		}
		map.put(MotifFor.ARRIVEE_HC, endOfMonth);
		map.put(MotifFor.DEPART_HC, endOfMonth);

		//
		// le permis C / nationalité est un cas particulier -> on prend la veille du prochain début de mois (la veille de la date courante si on est en début de mois)
		//
		if (passageSourceOrdinaire) {
			map.put(MotifFor.PERMIS_C_SUISSE, new PreviousDayDateShiftingStrategy(new NextBeginOfMonthDateShiftingStrategy()));
		}

		//
		// pas de décalage de date, mais des motifs suffisament impératifs pour contrer un décalage de date, justement
		//
		final NoopDateShiftingStrategy imperativeNoop = new NoopDateShiftingStrategy(true);
		map.put(MotifFor.ARRIVEE_HS, imperativeNoop);
		map.put(MotifFor.DEPART_HS, imperativeNoop);
		map.put(MotifFor.VEUVAGE_DECES, imperativeNoop);

		//
		// tous les autres -> Noop
		//
		final DateShiftingStrategy noop = new NoopDateShiftingStrategy(false);
		for (MotifFor motif : MotifFor.values()) {
			if (!map.containsKey(motif)) {
				map.put(motif, noop);
			}
		}
		return map;
	}

	/**
	 * Renvoie une liste triée par date des rapports entre tiers d'un type et d'un sens donné
	 * @param clazz la classe des rapports à trouver
	 * @param objet <code>true</code> si on doit rechercher dans la collection des rapports objets, <code>false</code> s'il s'agit des rapports sujets
	 * @param <T> type de rapport entre tiers
	 * @return la liste des rapports non-annulés trouvés
	 */
	private static <T extends RapportEntreTiers> List<T> getRapportsEntreTiers(PersonnePhysique pp, Class<T> clazz, boolean objet) {
		final Set<RapportEntreTiers> base = objet ? pp.getRapportsObjet() : pp.getRapportsSujet();
		final List<T> liste;
		if (base != null && !base.isEmpty()) {
			final List<T> tempList = new LinkedList<>();
			for (RapportEntreTiers r : base) {
				if (!r.isAnnule() && clazz.isAssignableFrom(r.getClass())) {
					//noinspection unchecked
					tempList.add((T) r);
				}
			}
			liste = new ArrayList<>(tempList);
			Collections.sort(liste, new DateRangeComparator<T>());
		}
		else {
			liste = Collections.emptyList();
		}
		return liste;
	}

	/**
	 * @param ctb un contribuable
	 * @return la liste des fors fiscaux principaux non-annulés de ce contribuables, triés par date
	 */
	private static List<ForFiscalPrincipal> getForsPrincipaux(Contribuable ctb) {
		return ctb.getForsFiscauxPrincipauxActifsSorted();
	}

	/**
	 * @param pp une personne physique
	 * @return l'ensemble des IDs des ménages communs liés à cette personnes physiques (en cas de re-mariage ou réconciliation, on s'assure que chaque ménage n'est présent qu'une seule fois)
	 */
	private static Set<Long> getIdsMenagesCommuns(PersonnePhysique pp) {
		final List<AppartenanceMenage> am = getRapportsEntreTiers(pp, AppartenanceMenage.class, false);
		final Set<Long> idsMenages = new HashSet<>();
		for (AppartenanceMenage lienMenage : am) {
			idsMenages.add(lienMenage.getObjetId());
		}
		return idsMenages;
	}

	@Override
	public List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp) {

		// j'ai besoin :
		// 1. des rapports de travail de la personne
		// 2. des fors de la personne et de ses ménages communs

		final List<ForFiscalPrincipal> fors = getForsPrincipaux(pp);
		final Set<Long> idsMenages = getIdsMenagesCommuns(pp);
		for (Long idMenage : idsMenages) {
			final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage, true);
			fors.addAll(getForsPrincipaux(mc));
		}

		// il est important que les fors soient triés y compris si plusieurs contribuables sont impliqués
		Collections.sort(fors, new DateRangeComparator<ForFiscalPrincipal>());

		final List<RapportPrestationImposable> rpis = getRapportsEntreTiers(pp, RapportPrestationImposable.class, false);
		return determine(pp, fors, rpis);
	}

	/**
	 * @param sortedList liste triée d'éléments
	 * @param <T> types des éléments de la liste
	 * @return le premier et le dernier (qui peuvent être les mêmes) éléments de la liste
	 */
	@Nullable
	private static <T extends DateRange> Pair<T, T> getFirstAndLast(List<T> sortedList) {
		if (sortedList.isEmpty()) {
			return null;
		}
		else {
			return Pair.of(sortedList.get(0), CollectionsUtils.getLastElement(sortedList));
		}
	}

	/**
	 * @param fors la liste des fors à considérer
	 * @param rpis la liste des rapports de travail à considérer
	 * @return une paire {min, max} sur les périodes fiscales concernées (<code>null</code> si tout est vide...)
	 */
	@Nullable
	private static Pair<Integer, Integer> getPeriodInterval(List<ForFiscalPrincipal> fors, List<RapportPrestationImposable> rpis) {
		final Pair<ForFiscalPrincipal, ForFiscalPrincipal> universeFors = getFirstAndLast(fors);
		final Pair<RapportPrestationImposable, RapportPrestationImposable> universeRpis = getFirstAndLast(rpis);
		if (universeFors == null && universeRpis == null) {
			return null;
		}

		final RegDate debut;
		final RegDate fin;
		if (universeFors == null) {
			debut = universeRpis.getLeft().getDateDebut();
			fin = universeRpis.getRight().getDateFin();
		}
		else if (universeRpis == null) {
			debut = universeFors.getLeft().getDateDebut();
			fin = universeFors.getRight().getDateFin();
		}
		else {
			debut = RegDateHelper.minimum(universeFors.getLeft().getDateDebut(), universeRpis.getLeft().getDateDebut(), NullDateBehavior.EARLIEST);
			fin = RegDateHelper.maximum(universeFors.getRight().getDateFin(), universeRpis.getRight().getDateFin(), NullDateBehavior.LATEST);
		}

		return Pair.of(debut.year(), fin == null ? RegDate.get().year() : fin.year());
	}

	/**
	 * Extraction des éléments d'une liste qui intersectent un range donné
	 * @param pf le range d'intersection
	 * @param src la liste à filtrer
	 * @param <T> le type des éléments de la liste
	 * @return une nouvelle liste contenant tous les éléments de la liste initiale qui intersectent avec le range donné (dans le même ordre que dans la liste initiale)
	 */
	private static <T extends DateRange> List<T> extractIntersectionWithFiscalPeriod(DateRange pf, List<T> src) {
		if (pf.getDateDebut() == null || !pf.getDateDebut().addYears(1).getOneDayBefore().equals(pf.getDateFin()) || pf.getDateDebut().year() != pf.getDateFin().year()) {
			throw new IllegalArgumentException("Invalid call with pf " + DateRangeHelper.toDisplayString(pf));
		}
		final List<T> res = new ArrayList<>(src.size());
		for (T range : src) {
			if (DateRangeHelper.intersect(range, pf)) {
				res.add(range);
			}
		}
		return res.isEmpty() ? Collections.<T>emptyList() : res;
	}

	/**
	 * Iterateur sur une liste qui permet de guigner l'élément juste avant et l'élément juste après
	 */
	private static final class ForIterator implements Iterator<ForFiscalPrincipal> {

		private int index = 0;      // l'index du prochain élément renvoyé par {@link next()}
		private final List<ForFiscalPrincipal> list;

		public ForIterator(List<ForFiscalPrincipal> list) {
			this.list = (list instanceof RandomAccess ? list : new ArrayList<>(list));
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < list.size();
		}

		@Override
		public ForFiscalPrincipal next() {
			return list.get(index ++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		public ForFiscalPrincipal peekAtPrevious() {
			// le précédent du dernier appel à next -> -2
			return index > 1 ? list.get(index - 2) : null;
		}

		public ForFiscalPrincipal peekAtNext() {
			return index < list.size() ? list.get(index) : null;
		}
	}

	/**
	 * Calcul du motif d'ouverture à considérer (en donnant la priorité aux changements d'autorité fiscales - en fait, aux changements de clé de localisation : canton/pays)
	 * @param motifOuverture le motif à utiliser s'il n'y a pas de changement de clé de localisation
	 * @param dateDebut date de début du for (= date du motif)
	 * @param typeAutoriteFiscale type d'autorité fiscale du for principal
	 * @param noOfsAutoriteFiscale numéro OFS de l'entité derrière le for principal
	 * @param typeAutoriteFiscalePrecedente type d'autorité fiscale du for principal précédent
	 * @param noOfsAutoriteFiscalePrecedente numéro OFS de l'entité derrière le for principal précédent
	 * @return le motif effectif à prendre en compte
	 */
	private MotifFor computeActualMotive(MotifFor motifOuverture, @NotNull RegDate dateDebut,
	                                     @NotNull TypeAutoriteFiscale typeAutoriteFiscale, int noOfsAutoriteFiscale,
	                                     @NotNull TypeAutoriteFiscale typeAutoriteFiscalePrecedente, int noOfsAutoriteFiscalePrecedente) {
		if (typeAutoriteFiscale == typeAutoriteFiscalePrecedente) {
			final String cleLocalisationAvant = PeriodeImpositionImpotSource.buildCleLocalisation(typeAutoriteFiscalePrecedente, noOfsAutoriteFiscalePrecedente,
			                                                                                      dateDebut.getOneDayBefore(), infraService);
			final String cleLocalisationApres = PeriodeImpositionImpotSource.buildCleLocalisation(typeAutoriteFiscale, noOfsAutoriteFiscale, dateDebut, infraService);
			if (cleLocalisationApres.equals(cleLocalisationAvant)) {
				return motifOuverture;
			}
			else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
				return MotifFor.DEPART_HS;
			}
			else {
				return MotifFor.DEPART_HC;
			}
		}
		else if (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS) {
			return MotifFor.DEPART_HS;
		}
		else if (typeAutoriteFiscalePrecedente == TypeAutoriteFiscale.PAYS_HS) {
			return MotifFor.ARRIVEE_HS;
		}
		else if (typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_HC) {
			return MotifFor.DEPART_HC;
		}
		else {
			return MotifFor.ARRIVEE_HC;
		}
	}

	private static Map<MotifFor, DateShiftingStrategy> getBeginDateShiftingStrategies(@Nullable ModeImposition before, ModeImposition after) {
		if ((before == ModeImposition.SOURCE || before == null) && !after.isSource()) {
			return BEGIN_DATE_SHIFTING_STRATEGIES_SRC_ORD;
		}
		else {
			return BEGIN_DATE_SHIFTING_STRATEGIES_DEFAULT;
		}
	}

	/**
	 * @param ffp for fiscal principal de base
	 * @param previous éventuel for principal précédent
	 * @return <ul><li>la date de début pour la PIIS du for</li><li>si oui ou non cette date peut même servir à contrer un décalage précédent</li></ul>
	 */
	private Pair<RegDate, Boolean> computeDateDebutMapping(ForFiscalPrincipal ffp, @Nullable ForFiscalPrincipal previous) {
		final ForFiscalPrincipal realPrevious = (previous != null && previous.getDateFin().getOneDayAfter() == ffp.getDateDebut() ? previous : null);
		final MotifFor actualMotive = realPrevious != null
				? computeActualMotive(ffp.getMotifOuverture(), ffp.getDateDebut(),
				                      ffp.getTypeAutoriteFiscale(), ffp.getNumeroOfsAutoriteFiscale(),
				                      previous.getTypeAutoriteFiscale(), previous.getNumeroOfsAutoriteFiscale())
				: ffp.getMotifOuverture();
		if (actualMotive != null) {
			final DateShiftingStrategy strategy = getBeginDateShiftingStrategies(realPrevious != null ? realPrevious.getModeImposition() : null, ffp.getModeImposition()).get(actualMotive);
			return Pair.of(strategy.shift(ffp.getDateDebut()), strategy.isImperativeSegmentationPoint());
		}
		else {
			return Pair.of(ffp.getDateDebut(), false);
		}
	}

	/**
	 * Appelé sur le dernier for de la liste (il n'y a donc jamais de for suivant...)
	 * @param ffp for fiscal principal de base
	 * @return <ul><li>la date de fin pour la PIIS du for</li><li>si oui ou non cette date peut même servir à contrer un décalage précédent</li></ul>
	 */
	private Pair<RegDate, Boolean> computeDateFinMapping(ForFiscalPrincipal ffp) {
		final DateShiftingStrategy strategy = END_DATE_SHIFTING_STRATEGIES_DEFAULT.get(ffp.getMotifFermeture());
		return Pair.of(strategy.shift(ffp.getDateFin()), strategy.isImperativeSegmentationPoint());
	}

	/**
	 * Calcule un mapping entre les dates des fors et les dates des périodes d'imposition IS
	 * @param fors les fors, ordonnés par date, qui ont une intersection avec la PF donnée
	 * @param pf la période fiscale qui nous intéresse
	 * @return le mapping des dates
	 */
	private Map<RegDate, RegDate> computeDateMapping(List<ForFiscalPrincipal> fors, DateRange pf) {
		final Map<RegDate, RegDate> debutMapping = new HashMap<>(fors.size());
		final Map<RegDate, RegDate> finMapping = new HashMap<>(fors.size());
		final ForIterator iterator = new ForIterator(fors);
		while (iterator.hasNext()) {
			final ForFiscalPrincipal ffp = iterator.next();
			if (!pf.isValidAt(ffp.getDateDebut())) {
				debutMapping.put(ffp.getDateDebut(), pf.getDateDebut());
			}
			else {
				final Pair<RegDate, Boolean> computedMapping = computeDateDebutMapping(ffp, iterator.peekAtPrevious());
				final RegDate newDebut = computedMapping.getLeft();
				debutMapping.put(ffp.getDateDebut(), newDebut);
				finMapping.put(ffp.getDateDebut().getOneDayBefore(), newDebut.getOneDayBefore());   // peut écraser une valeur précédemment placée là dans une boucle précédente

				// si on rencontre un mapping "impératif", il faut contrer les éventuels mappings qui allaient plus loin
				// (comme on voit les fors dans l'ordre chronologique et que seuls les NOOP peuvent être "impératifs",
				// on peut se contenter de travailler sur les dates déjà connues)
				if (computedMapping.getRight()) {
					for (Map.Entry<RegDate, RegDate> mapping : debutMapping.entrySet()) {
						if (mapping.getValue().isAfter(newDebut)) {
							mapping.setValue(newDebut);
						}
					}
					for (Map.Entry<RegDate, RegDate> mapping : finMapping.entrySet()) {
						if (mapping.getValue().isAfterOrEqual(newDebut)) {
							mapping.setValue(newDebut.getOneDayBefore());
						}
					}
				}
			}

			if (!pf.isValidAt(ffp.getDateFin())) {
				finMapping.put(ffp.getDateFin(), pf.getDateFin());
			}
			else {
				final ForFiscalPrincipal next = iterator.peekAtNext();
				if (next != null) {
					finMapping.put(ffp.getDateFin(), ffp.getDateFin());     // pourra être écrasé lors d'un passage ultérieur dans la boucle
				}
				else {
					final Pair<RegDate, Boolean> computedMapping = computeDateFinMapping(ffp);
					final RegDate newFin = computedMapping.getLeft();
					finMapping.put(ffp.getDateFin(), newFin);

					// mapping impératif sur la fermeture du dernier for ?
					if (computedMapping.getRight()) {
						for (Map.Entry<RegDate, RegDate> mapping : finMapping.entrySet()) {
							if (mapping.getValue().isAfterOrEqual(newFin)) {
								mapping.setValue(newFin);
							}
						}
					}
				}
			}
		}

		final Map<RegDate, RegDate> mapping = new HashMap<>(fors.size() * 2);
		mapping.putAll(finMapping);
		mapping.putAll(debutMapping);
		return mapping;
	}

	/**
	 * @param ffp un for principal
	 * @param forSuivant l'éventuel for principal suivant
	 * @return si oui ou non on peut considérer que le for principal se termine avec un départ HC
	 */
	private boolean isDepartHC(ForFiscalPrincipal ffp, @Nullable ForFiscalPrincipal forSuivant) {
		final MotifFor motive;
		if (forSuivant != null && forSuivant.getDateDebut().getOneDayBefore() == ffp.getDateFin()) {
			motive = computeActualMotive(forSuivant.getMotifOuverture(), forSuivant.getDateDebut(), forSuivant.getTypeAutoriteFiscale(), forSuivant.getNumeroOfsAutoriteFiscale(),
			                             ffp.getTypeAutoriteFiscale(), ffp.getNumeroOfsAutoriteFiscale());
		}
		else {
			motive = ffp.getMotifFermeture();
		}
		return motive == MotifFor.DEPART_HC;
	}

	/**
	 * @param ffp un for principal
	 * @param forSuivant l'éventuel for principal suivant
	 * @return le type ({@link PeriodeImpositionImpotSource.Type#MIXTE MIXTE} ou {@link PeriodeImpositionImpotSource.Type#SOURCE SOURCE}) de la période d'imposition IS à créer pour le for principal
	 */
	private PeriodeImpositionImpotSource.Type determineTypePeriode(ForFiscalPrincipal ffp, @Nullable ForFiscalPrincipal forSuivant) {
		final PeriodeImpositionImpotSource.Type type;
		if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && ffp.getModeImposition() != ModeImposition.MIXTE_137_2 && isDepartHC(ffp, forSuivant)) {
			type = PeriodeImpositionImpotSource.Type.SOURCE;
		}
		else if (ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD || ffp.getModeImposition() == ModeImposition.SOURCE) {
			type = PeriodeImpositionImpotSource.Type.SOURCE;
		}
		else {
			type = PeriodeImpositionImpotSource.Type.MIXTE;
		}
		return type;
	}

	/**
	 * La méthode centrale du calcul des périodes d'imposition IS
	 * @param pp personne physique dont on veut calculer les périodes d'imposition IS
	 * @param fors les fors principaux non-annulés de cette personne physique (et de ses éventuels ménages communs), triés par date
	 * @param rpis les rapports de travail non-annulés de cette personne physique, triés par date
	 * @return la liste des périodes d'imposition IS de la personne physique considérée
	 */
	private List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp, List<ForFiscalPrincipal> fors, List<RapportPrestationImposable> rpis) {

		// cas trivial de la personne sans for ni RT (= mineur ?)
		final Pair<Integer, Integer> interval = getPeriodInterval(fors, rpis);
		if (interval == null) {
			// cas trivial de la personne sans for ni RT (= mineur ?)
			return Collections.emptyList();
		}

		// on avance pf par pf
		final int size = interval.getRight() - interval.getLeft() + 1;
		final List<PeriodeImpositionImpotSource> piis = new ArrayList<>(size);
		for (int pf = interval.getLeft() ; pf <= interval.getRight() ; ++ pf) {
			final DateRange pfRange = new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31));
			final List<ForFiscalPrincipal> forsPf = extractIntersectionWithFiscalPeriod(pfRange, fors);
			final List<RapportPrestationImposable> rpisPf = extractIntersectionWithFiscalPeriod(pfRange, rpis);
			if (forsPf.isEmpty() && rpisPf.isEmpty()) {
				// rien ici... PF suivante !
				continue;
			}

			if (forsPf.isEmpty()) {
				// il n'y a que des rapports de travail sur cette période -> toute la PF passe à la source
				piis.add(new PeriodeImpositionImpotSource(pp, PeriodeImpositionImpotSource.Type.SOURCE, pfRange.getDateDebut(), pfRange.getDateFin(), null, infraService));
				continue;
			}

			// s'il n'y a pas de rapports de travail, il faut qu'il y ait au moins un for "source/mixte" vaudois pour générer une période d'imposition IS
			if (rpisPf.isEmpty()) {
				boolean forAssimileSourceTrouve = false;
				for (ForFiscalPrincipal ffp : forsPf) {
					if (ffp.getModeImposition().isSource() && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						forAssimileSourceTrouve = true;
						break;
					}
				}
				if (!forAssimileSourceTrouve) {
					// rien dans cette période....
					continue;
				}
			}

			// il y a des fors, nous voici donc dans le vif du sujet...
			final Map<RegDate, RegDate> dateMapping = computeDateMapping(fors, pfRange);

			// tous les fors sont pris en compte
			final List<PeriodeImpositionImpotSource> piisPf = new ArrayList<>(forsPf.size());
			RegDate lastDebut = null;
			final ForIterator iterator = new ForIterator(forsPf);
			while (iterator.hasNext()) {
				final ForFiscalPrincipal ffp = iterator.next();
				final RegDate debut = RegDateHelper.maximum(dateMapping.get(ffp.getDateDebut()), lastDebut, NullDateBehavior.EARLIEST);
				final RegDate fin = dateMapping.get(ffp.getDateFin());
				if (debut.isBeforeOrEqual(fin)) {
					final PeriodeImpositionImpotSource.Type type = determineTypePeriode(ffp, iterator.peekAtNext());
					piisPf.add(new PeriodeImpositionImpotSource(pp, type, debut, fin, ffp, infraService));
				}
				lastDebut = debut;
			}

			// il faut remplir les trous avec des périodes "SOURCE" sur toute la période (sauf en cas de décès)
			final RegDate dateDeces = tiersService.getDateDeces(pp);
			final RegDate dateFin = RegDateHelper.minimum(pfRange.getDateFin(), dateDeces, NullDateBehavior.LATEST);
			if (RegDateHelper.isBeforeOrEqual(pfRange.getDateDebut(), dateFin, NullDateBehavior.LATEST)) {
				final List<PeriodeImpositionImpotSource> fonds = new ArrayList<>(1);
				fonds.add(new PeriodeImpositionImpotSource(pp, PeriodeImpositionImpotSource.Type.SOURCE, pfRange.getDateDebut(), dateFin, null, infraService));

				piis.addAll(DateRangeHelper.override(fonds, piisPf,
				                                     new DateRangeHelper.AdapterCallbackExtended<PeriodeImpositionImpotSource>() {
					                                     @Override
					                                     public PeriodeImpositionImpotSource adapt(PeriodeImpositionImpotSource range, RegDate debut, PeriodeImpositionImpotSource sourceSurchargeDebut,
					                                                                               RegDate fin, PeriodeImpositionImpotSource sourceSurchargeFin) {
						                                     return new PeriodeImpositionImpotSource(range, sourceSurchargeDebut != null ? debut : range.getDateDebut(), sourceSurchargeFin != null ? fin : range.getDateFin());
					                                     }

					                                     @Override
					                                     public PeriodeImpositionImpotSource duplicate(PeriodeImpositionImpotSource range) {
						                                     return range.duplicate();
					                                     }

					                                     @Override
					                                     public PeriodeImpositionImpotSource adapt(PeriodeImpositionImpotSource range, RegDate debut, RegDate fin) {
						                                     throw new IllegalArgumentException("Should not be called");
					                                     }
				                                     }));
			}
		}

		return piis.isEmpty() ? Collections.<PeriodeImpositionImpotSource>emptyList() : DateRangeHelper.collate(piis);
	}
}
