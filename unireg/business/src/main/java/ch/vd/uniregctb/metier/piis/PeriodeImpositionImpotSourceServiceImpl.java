package ch.vd.uniregctb.metier.piis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementHelper;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
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

	private static final Set<MotifFor> SHIFT_TO_END_OF_MONTH = EnumSet.of(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MotifFor.PERMIS_C_SUISSE, MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC);

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
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

	private static List<ForFiscalPrincipal> getForsPrincipaux(Contribuable ctb) {
		return ctb.getForsFiscauxPrincipauxActifsSorted();
	}

	@Override
	public List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp) throws AssujettissementException {

		// j'ai besoin :
		// 1. des rapports de travail de la personne
		// 2. des fors de la personne et de ses ménages communs

		final List<ForFiscalPrincipal> fors = getForsPrincipaux(pp);
		final List<AppartenanceMenage> am = getRapportsEntreTiers(pp, AppartenanceMenage.class, false);
		for (AppartenanceMenage lienMenage : am) {
			final MenageCommun mc = (MenageCommun) tiersDAO.get(lienMenage.getObjetId(), true);
			fors.addAll(getForsPrincipaux(mc));
		}

		// il est important que les fors soient triés y compris si plusieurs contribuables sont impliqués
		Collections.sort(fors, new DateRangeComparator<ForFiscalPrincipal>());

		final List<RapportPrestationImposable> rpis = getRapportsEntreTiers(pp, RapportPrestationImposable.class, false);
		return determine(pp, fors, rpis);
	}

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

	private static boolean shouldShiftToEndOfMonth(MotifFor motifOuverture) {
		return SHIFT_TO_END_OF_MONTH.contains(motifOuverture);
	}

	private static RegDate computeDateDebutMapping(ForFiscalPrincipal ffp) {
		if (shouldShiftToEndOfMonth(ffp.getMotifOuverture())) {
			return ffp.getDateDebut().getLastDayOfTheMonth().getOneDayAfter();
		}
		else {
			return ffp.getDateDebut();
		}
	}

	private static Map<RegDate, RegDate> computeDateMapping(List<ForFiscalPrincipal> fors, DateRange pf) {
		final Map<RegDate, RegDate> mapping = new HashMap<>(fors.size() * 2);
		for (ForFiscalPrincipal ffp : fors) {
			if (!pf.isValidAt(ffp.getDateDebut())) {
				mapping.put(ffp.getDateDebut(), pf.getDateDebut());
			}
			else {
				final RegDate newDebut = computeDateDebutMapping(ffp);
				mapping.put(ffp.getDateDebut(), newDebut);
				mapping.put(ffp.getDateDebut().getOneDayBefore(), newDebut.getOneDayBefore());
			}

			if (!pf.isValidAt(ffp.getDateFin())) {
				mapping.put(ffp.getDateFin(), pf.getDateFin());
			}
			else {
				mapping.put(ffp.getDateFin(), ffp.getDateFin());
			}
		}
		return mapping;
	}

	private static PeriodeImpositionImpotSource.Type determineTypePeriode(ForFiscalPrincipal ffp, List<Assujettissement> assujettissementsPf) {
		final PeriodeImpositionImpotSource.Type type;
		if ((assujettissementsPf.isEmpty() && ffp.getTiers() instanceof PersonnePhysique)
				|| ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
				|| ffp.getModeImposition() == ModeImposition.SOURCE) {
			type = PeriodeImpositionImpotSource.Type.SOURCE;
		}
		else {
			type = PeriodeImpositionImpotSource.Type.MIXTE;
		}
		return type;
	}

	private static List<Assujettissement> extractAssujettissementOrdinaireVaudois(List<Assujettissement> list, int pf) {
		final List<Assujettissement> onPf = AssujettissementHelper.extractYear(list, pf);
		if (onPf != null) {
			final Iterator<Assujettissement> iterator = onPf.iterator();
			while (iterator.hasNext()) {
				final Assujettissement a = iterator.next();
				if (a instanceof SourcierPur || a instanceof HorsCanton) {
					iterator.remove();
				}
			}
			return onPf;
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp, List<ForFiscalPrincipal> fors, List<RapportPrestationImposable> rpis) throws AssujettissementException {

		// cas trivial de la personne sans for ni RT (= mineur ?)
		final Pair<Integer, Integer> interval = getPeriodInterval(fors, rpis);
		if (interval == null) {
			// cas trivial de la personne sans for ni RT (= mineur ?)
			return Collections.emptyList();
		}

		// calcul de l'assujettissement ordinaire
		final List<Assujettissement> assujettissements = assujettissementService.determine(pp);

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
				piis.add(new PeriodeImpositionImpotSource(pp, PeriodeImpositionImpotSource.Type.SOURCE, pfRange.getDateDebut(), pfRange.getDateFin(), null));
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

			// quel est l'assujettissement ordinaire vaudois sur cette PF ?
			final List<Assujettissement> assujettissementPf = extractAssujettissementOrdinaireVaudois(assujettissements, pf);

			// il y a des fors, nous voici donc dans le vif du sujet...
			final Map<RegDate, RegDate> dateMapping = computeDateMapping(fors, pfRange);

			// tous les fors sont pris en compte
			final List<PeriodeImpositionImpotSource> piisPf = new ArrayList<>(forsPf.size());
			RegDate lastDebut = null;
			for (ForFiscalPrincipal ffp : forsPf) {
				final RegDate debut = RegDateHelper.maximum(dateMapping.get(ffp.getDateDebut()), lastDebut, NullDateBehavior.EARLIEST);
				final RegDate fin = dateMapping.get(ffp.getDateFin());
				if (debut.isBeforeOrEqual(fin)) {
					final PeriodeImpositionImpotSource.Type type = determineTypePeriode(ffp, assujettissementPf);
					piisPf.add(new PeriodeImpositionImpotSource(pp, type, debut, fin, ffp));
				}
				lastDebut = debut;
			}

			// il faut remplir les trous avec des périodes "SOURCE" sur toute la période (sauf en cas de décès)
			final RegDate dateDeces = tiersService.getDateDeces(pp);
			final RegDate dateFin = RegDateHelper.minimum(pfRange.getDateFin(), dateDeces, NullDateBehavior.LATEST);
			if (RegDateHelper.isBeforeOrEqual(pfRange.getDateDebut(), dateFin, NullDateBehavior.LATEST)) {
				final List<PeriodeImpositionImpotSource> fonds = new ArrayList<>(1);
				fonds.add(new PeriodeImpositionImpotSource(pp, PeriodeImpositionImpotSource.Type.SOURCE, pfRange.getDateDebut(), dateFin, null));

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
