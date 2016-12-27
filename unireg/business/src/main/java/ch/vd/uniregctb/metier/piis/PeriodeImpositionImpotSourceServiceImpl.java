package ch.vd.uniregctb.metier.piis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
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
			Collections.sort(liste, new DateRangeComparator<>());
		}
		else {
			liste = Collections.emptyList();
		}
		return liste;
	}

	/**
	 * @param ctb un contribuable
	 * @param rw <code>true</code> si la collection retournée doit être accessible en écriture, <code>false</code> si lecture seule suffit
	 * @return la liste des fors fiscaux principaux non-annulés de ce contribuables, triés par date
	 */
	@NotNull
	private static List<ForFiscalPrincipalPP> getForsPrincipaux(ContribuableImpositionPersonnesPhysiques ctb, boolean rw) {
		final List<ForFiscalPrincipalPP> ffps = ctb.getForsFiscauxPrincipauxActifsSorted();
		return ffps != null ? ffps : (rw ? new ArrayList<>() : Collections.emptyList());
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
	public List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp) throws PeriodeImpositionImpotSourceServiceException {

		// j'ai besoin :
		// 1. des rapports de travail de la personne
		// 2. des fors de la personne et de ses ménages communs

		final List<ForFiscalPrincipalPP> fors = getForsPrincipaux(pp, true);
		final Set<Long> idsMenages = getIdsMenagesCommuns(pp);
		for (Long idMenage : idsMenages) {
			final MenageCommun mc = (MenageCommun) tiersDAO.get(idMenage, true);
			fors.addAll(getForsPrincipaux(mc, false));
		}

		// il est important que les fors soient triés y compris si plusieurs contribuables sont impliqués
		Collections.sort(fors, new DateRangeComparator<ForFiscalPrincipal>());
		ForFiscalPrincipal previous = null;
		for (ForFiscalPrincipal ffp : fors) {
			// il y a chevauchement de fors (ce qui est normalement interdit sur les fors principaux) si la date de début du for courant
			// est incluse dans le for vu juste avant
			if (previous != null && previous.isValidAt(ffp.getDateDebut())) {
				final String msg = String.format("Chevauchement de fors principaux : %s sur contribuable %s avec %s sur contribuable %s",
				                                 previous, FormatNumeroHelper.numeroCTBToDisplay(previous.getTiers().getNumero()),
				                                 ffp, FormatNumeroHelper.numeroCTBToDisplay(ffp.getTiers().getNumero()));

				throw new PeriodeImpositionImpotSourceServiceException(msg);
			}
			previous = ffp;
		}

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
	private static Pair<Integer, Integer> getPeriodInterval(List<ForFiscalPrincipalPP> fors, List<RapportPrestationImposable> rpis) {
		final Pair<ForFiscalPrincipalPP, ForFiscalPrincipalPP> universeFors = getFirstAndLast(fors);
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
		return res.isEmpty() ? Collections.emptyList() : res;
	}

	/**
	 * Calcul du motif d'ouverture à considérer (en donnant la priorité aux changements d'autorité fiscales - en fait, aux changements de clé de localisation : canton/pays)
	 * @param motifFermeture motif de fermeture du for précédent
	 * @param motifOuverture motif d'ouverture du for
	 * @param dateDebut date de début du for (= date du motif)
	 * @param typeAutoriteFiscale type d'autorité fiscale du for principal
	 * @param noOfsAutoriteFiscale numéro OFS de l'entité derrière le for principal
	 * @param typeAutoriteFiscalePrecedente type d'autorité fiscale du for principal précédent
	 * @param noOfsAutoriteFiscalePrecedente numéro OFS de l'entité derrière le for principal précédent
	 * @return le motif effectif à prendre en compte
	 */
	private MotifFor computeActualMotive(MotifFor motifFermeture, MotifFor motifOuverture, @NotNull RegDate dateDebut,
	                                     @NotNull TypeAutoriteFiscale typeAutoriteFiscale, int noOfsAutoriteFiscale,
	                                     @NotNull TypeAutoriteFiscale typeAutoriteFiscalePrecedente, int noOfsAutoriteFiscalePrecedente) {
		final Localisation localisationAvant = Localisation.get(noOfsAutoriteFiscalePrecedente, dateDebut.getOneDayBefore(), typeAutoriteFiscalePrecedente, infraService);
		final Localisation localisationApres = Localisation.get(noOfsAutoriteFiscale, dateDebut, typeAutoriteFiscale, infraService);
		return FractionnementsPeriodesImpositionIS.getMotifEffectif(localisationAvant, motifFermeture, localisationApres, motifOuverture);
	}

	/**
	 * @param ffp un for principal
	 * @param forSuivant l'éventuel for principal suivant
	 * @param pf période fiscale concernée par la PIIS en cours de construction
	 * @return si oui ou non on peut considérer que le for principal se termine avec un départ HC
	 */
	private boolean isDepartHC(ForFiscalPrincipal ffp, @Nullable ForFiscalPrincipal forSuivant, int pf) {
		final MotifFor motive;
		if (RegDateHelper.isAfterOrEqual(ffp.getDateFin(), RegDate.get(pf, 12, 31), NullDateBehavior.LATEST)) {
			motive = null;
		}
		else if (forSuivant != null && forSuivant.getDateDebut().getOneDayBefore() == ffp.getDateFin()) {
			motive = computeActualMotive(ffp.getMotifFermeture(), forSuivant.getMotifOuverture(), forSuivant.getDateDebut(), forSuivant.getTypeAutoriteFiscale(), forSuivant.getNumeroOfsAutoriteFiscale(),
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
	 * @param pf la période fiscale concernée par la PIIS en construction
	 * @return le type ({@link ProtoPeriodeImpositionImpotSource.Type#MIXTE MIXTE}, {@link ProtoPeriodeImpositionImpotSource.Type#SOURCE SOURCE}
	 * ou {@link ProtoPeriodeImpositionImpotSource.Type#MIXTE_COMMUE_EN_SOURCE MIXTE_COMMUE_EN_SOURCE}) qui sera utilisé pour au final connaître
	 * le type réel de la période d'imposition IS à créer pour le for principal
	 */
	private ProtoPeriodeImpositionImpotSource.Type determineTypePeriode(ForFiscalPrincipalPP ffp, @Nullable ForFiscalPrincipal forSuivant, int pf) {
		final ProtoPeriodeImpositionImpotSource.Type type;
		if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			if (ffp.getModeImposition() == ModeImposition.SOURCE) {
				type = ProtoPeriodeImpositionImpotSource.Type.SOURCE;
			}
			else if (ffp.getModeImposition() != ModeImposition.MIXTE_137_2 && isDepartHC(ffp, forSuivant, pf)) {
				type = ProtoPeriodeImpositionImpotSource.Type.MIXTE_COMMUE_EN_SOURCE;
			}
			else {
				type = ProtoPeriodeImpositionImpotSource.Type.MIXTE;
			}
		}
		else {
			type = ProtoPeriodeImpositionImpotSource.Type.SOURCE;
		}
		return type;
	}

	private static RegDate determineDateDebut(ForFiscalPrincipal ffp, int pf, FractionnementsPeriodesImpositionIS fractionnements) {
		final RegDate dateDebut = ffp.getDateDebut();
		if (dateDebut.year() < pf) {
			return RegDate.get(pf, 1, 1);
		}

		final Fraction fraction = fractionnements.getAt(dateDebut);
		if (fraction != null) {
			return fraction.getDate();
		}

		return dateDebut;
	}

	private static RegDate determineDateFin(ForFiscalPrincipal ffp, int pf, FractionnementsPeriodesImpositionIS fractionnements) {
		final RegDate dateFin = ffp.getDateFin();
		if (dateFin == null || dateFin.year() > pf) {
			return RegDate.get(pf, 12, 31);
		}

		final RegDate lendemainDateFin = dateFin.getOneDayAfter();
		final Fraction fraction = fractionnements.getAt(lendemainDateFin);
		if (fraction != null) {
			return fraction.getDate().getOneDayBefore();
		}

		return dateFin;
	}

	private static final class ProtoPeriodeImpositionImpotSource extends AbstractCollatablePeriodeImpositionImpotSource<ProtoPeriodeImpositionImpotSource.Type> {

		public enum Type {
			/**
			 * Source, sans aucun doute
			 */
			SOURCE {
				@Override
				public PeriodeImpositionImpotSource.Type toFinalType() {
					return PeriodeImpositionImpotSource.Type.SOURCE;
				}
			},
			/**
			 * Mixte, sans aucun doute
			 */
			MIXTE {
				@Override
				public PeriodeImpositionImpotSource.Type toFinalType() {
					return PeriodeImpositionImpotSource.Type.MIXTE;
				}
			},
			/**
			 * Mixte commué en Source, et susceptible de redevenir mixte si certaines conditions sont remplies
			 */
			MIXTE_COMMUE_EN_SOURCE {
				@Override
				public PeriodeImpositionImpotSource.Type toFinalType() {
					return PeriodeImpositionImpotSource.Type.SOURCE;
				}
			};

			public abstract PeriodeImpositionImpotSource.Type toFinalType();
		}

		private ProtoPeriodeImpositionImpotSource(PersonnePhysique pp, Type type, ForFiscalPrincipal forFiscal,
		                                          RegDate dateDebut, RegDate dateFin, Fraction fractionDebut, Fraction fractionFin,
		                                          ServiceInfrastructureService infraService) {
			super(pp, type, dateDebut, dateFin,
			      forFiscal != null ? forFiscal.getTypeAutoriteFiscale() : null, forFiscal != null ? forFiscal.getNumeroOfsAutoriteFiscale() : null,
			      Localisation.get(forFiscal, infraService), fractionDebut, fractionFin);
		}

		private ProtoPeriodeImpositionImpotSource(ProtoPeriodeImpositionImpotSource courant, ProtoPeriodeImpositionImpotSource suivant) {
			super(courant, suivant);
		}

		private ProtoPeriodeImpositionImpotSource(ProtoPeriodeImpositionImpotSource src, int noOfs) {
			super(src.getContribuable(), src.getType(), src.getDateDebut(), src.getDateFin(),
			      src.getTypeAutoriteFiscale(), noOfs,
			      src.getLocalisation(), src.getFractionDebut(), src.getFractionFin());
		}

		private ProtoPeriodeImpositionImpotSource(ProtoPeriodeImpositionImpotSource src, Type type) {
			super(src.getContribuable(), type, src.getDateDebut(), src.getDateFin(),
			      src.getTypeAutoriteFiscale(), src.getNoOfs(),
			      src.getLocalisation(), src.getFractionDebut(), src.getFractionFin());
		}

		public boolean isValid() {
			return getDateDebut().compareTo(getDateFin()) <= 0;
		}

		public PeriodeImpositionImpotSource project() {
			return new PeriodeImpositionImpotSource(getContribuable(), getType().toFinalType(), getDateDebut(), getDateFin(),
			                                        getTypeAutoriteFiscale(), getNoOfs(),
			                                        getLocalisation(), getFractionDebut(), getFractionFin());
		}

		public ProtoPeriodeImpositionImpotSource withNoOfs(int noOfs) {
			return new ProtoPeriodeImpositionImpotSource(this, noOfs);
		}

		public ProtoPeriodeImpositionImpotSource withType(Type type) {
			return new ProtoPeriodeImpositionImpotSource(this, type);
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return isValid() && super.isValidAt(date);
		}

		@Override
		public DateRange collate(DateRange next) {
			return new ProtoPeriodeImpositionImpotSource(this, (ProtoPeriodeImpositionImpotSource) next);
		}
	}

	/**
	 * La méthode centrale du calcul des périodes d'imposition IS
	 * @param pp personne physique dont on veut calculer les périodes d'imposition IS
	 * @param fors les fors principaux non-annulés de cette personne physique (et de ses éventuels ménages communs), triés par date
	 * @param rpis les rapports de travail non-annulés de cette personne physique, triés par date
	 * @return la liste des périodes d'imposition IS de la personne physique considérée
	 */
	private List<PeriodeImpositionImpotSource> determine(PersonnePhysique pp, List<ForFiscalPrincipalPP> fors, List<RapportPrestationImposable> rpis) {

		// cas trivial de la personne sans for ni RT (= mineur ?)
		final Pair<Integer, Integer> interval = getPeriodInterval(fors, rpis);
		if (interval == null) {
			// cas trivial de la personne sans for ni RT (= mineur ?)
			return Collections.emptyList();
		}

		// on avance pf par pf
		final List<PeriodeImpositionImpotSource> piis = new ArrayList<>();
		for (int pf = interval.getLeft() ; pf <= interval.getRight() ; ++ pf) {
			final DateRange pfRange = new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31));
			final List<ForFiscalPrincipalPP> forsPf = extractIntersectionWithFiscalPeriod(pfRange, fors);
			final List<RapportPrestationImposable> rpisPf = extractIntersectionWithFiscalPeriod(pfRange, rpis);
			if (forsPf.isEmpty() && rpisPf.isEmpty()) {
				// rien ici... PF suivante !
				continue;
			}

			if (forsPf.isEmpty()) {
				// il n'y a que des rapports de travail sur cette période -> toute la PF passe à la source
				piis.add(new PeriodeImpositionImpotSource(pp, pfRange.getDateDebut(), pfRange.getDateFin()));
				continue;
			}

			// s'il n'y a pas de rapports de travail, il faut qu'il y ait au moins un for "source/mixte" vaudois pour générer une période d'imposition IS
			if (rpisPf.isEmpty()) {
				boolean forAssimileSourceTrouve = false;
				for (ForFiscalPrincipalPP ffp : forsPf) {
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

			// [SIFISC-21550] comme on n'utilise qu'une partie des fors à la fois (= ceux qui intersectent l'année de la PF), il nous manque parfois
			// le for juste avant ou juste après (en cas de début de for au 01.01 ou de fin de for au 31.12), ce qui peut-être assez embêtant quand
			// il s'agit de rétablir la vérité sur les motifs d'ouverture ou de fermeture des fors en fonction des types d'autorité fiscale
			final ForFiscalPrincipalPP forAvantPF;
			if (forsPf.get(0).getDateDebut() == RegDate.get(pf, 1, 1)) {
				forAvantPF = DateRangeHelper.rangeAt(fors, RegDate.get(pf - 1, 12, 31));
			}
			else {
				forAvantPF = null;
			}
			final ForFiscalPrincipalPP forApresPF;
			if (CollectionsUtils.getLastElement(forsPf).getDateFin() == RegDate.get(pf, 12, 31)) {
				forApresPF = DateRangeHelper.rangeAt(fors, RegDate.get(pf + 1, 1, 1));
			}
			else {
				forApresPF = null;
			}

			final List<ProtoPeriodeImpositionImpotSource> protos = new ArrayList<>(forsPf.size());
			final FractionnementsPeriodesImpositionIS fracs = new FractionnementsPeriodesImpositionIS(forsPf, pf, forAvantPF, forApresPF, infraService);
			final MovingWindow<ForFiscalPrincipalPP> iterator = new MovingWindow<>(forsPf);
			while (iterator.hasNext()) {
				final MovingWindow.Snapshot<ForFiscalPrincipalPP> snapshot = iterator.next();
				final ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal = new ForFiscalPrincipalContext<>(snapshot);

				final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
				final ProtoPeriodeImpositionImpotSource.Type type = determineTypePeriode(current, forPrincipal.getNext(), pf);
				final RegDate dateDebut = determineDateDebut(current, pf, fracs);
				final Fraction fractionDebut = fracs.getAt(current.getDateDebut());
				final RegDate dateFin = determineDateFin(current, pf, fracs);
				final Fraction fractionFin = fracs.getAt(current.getDateFin());
				protos.add(new ProtoPeriodeImpositionImpotSource(pp, type, current, dateDebut, dateFin, fractionDebut, fractionFin, infraService));
			}

			// pour chaque localisation continue, toutes les périodes doivent prendre LE DERNIER FOR de la localisation
			Localisation localisationCourante = null;
			Integer noOfsCourant = null;
			RegDate dateFinAttendue = null;
			for (int i = protos.size() - 1 ; i >= 0 ; -- i) {
				final ProtoPeriodeImpositionImpotSource proto = protos.get(i);
				if (localisationCourante == null || !localisationCourante.equals(proto.getLocalisation()) || (dateFinAttendue != null && dateFinAttendue != proto.getDateFin())) {
					localisationCourante = proto.getLocalisation();
					noOfsCourant = proto.getNoOfs();
				}
				else if (noOfsCourant != null && !noOfsCourant.equals(proto.getNoOfs())) {
					protos.set(i, proto.withNoOfs(noOfsCourant));
				}
				dateFinAttendue = proto.getDateDebut().getOneDayBefore();
			}

			// supprimons les périodes invalides
			protos.removeIf(proto -> !proto.isValid());

			// [SIFISC-12981] on regroupe déjà les proto-périodes
			final List<ProtoPeriodeImpositionImpotSource> collatedProtos = collate(protos);

			// [SIFISC-12326] passage éventuel d'une période vaudoise SOURCE à MIXTE en fonction des autres périodes de la PF
			// 1. on commence à la fin et on cherche toutes les périodes mixtes (vaudoises, forcément)
			// 2. pour chacune d'entre elles, on recule encore jusqu'à trouver une autre période vaudoise mixte (on peut s'arrêter, le cas sera traité par une autre étape),
			//      ou un trou, ou une période HS, ou un fractionnement qui correspond à un passage au rôle
			// 3. les PF vaudoises "SOURCE" trouvées en passant deviennent "MIXTE"
			final Set<MotifFor> motifsObtentionRole = EnumSet.of(MotifFor.PERMIS_C_SUISSE,
			                                                     MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                                                     MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
			                                                     MotifFor.VEUVAGE_DECES);
			for (int i = collatedProtos.size() - 1 ; i >= 0 ; -- i) {
				final ProtoPeriodeImpositionImpotSource proto = collatedProtos.get(i);
				if (proto.getType() == ProtoPeriodeImpositionImpotSource.Type.MIXTE) {
					for (int j = i - 1 ; j >= 0 ; -- j) {
						final ProtoPeriodeImpositionImpotSource next = (j == i - 1 ? proto : collatedProtos.get(j + 1));
						final ProtoPeriodeImpositionImpotSource current = collatedProtos.get(j);
						if (!DateRangeHelper.isCollatable(current, next)                            // il y a un trou, là...
								|| current.getType() == ProtoPeriodeImpositionImpotSource.Type.MIXTE     // on verra dans la prochaine boucle de "i"
								|| current.getLocalisation().isInconnue()                                // trou bouché ?
								|| current.getLocalisation().isHS()                                      // passage par hors-Suisse
								|| (current.getFractionFin() != null && motifsObtentionRole.contains(current.getFractionFin().getMotif()))) {       // passage de SOURCE à MIXTE normal

							// pas la peine d'aller plus loin depuis cette période-là (i)
							break;
						}

						// si la localisation est vaudoise, on transforme le type SOURCE en MIXTE
						if (current.getLocalisation().isVD()) {
							if (current.getType() == ProtoPeriodeImpositionImpotSource.Type.MIXTE_COMMUE_EN_SOURCE) {
								collatedProtos.set(j, current.withType(ProtoPeriodeImpositionImpotSource.Type.MIXTE));
							}
							break;
						}
					}
				}
			}

			// maintenant on peut projeter les proto-périodes en véritables périodes
			final List<PeriodeImpositionImpotSource> piisPf = new ArrayList<>(collatedProtos.size());
			for (ProtoPeriodeImpositionImpotSource proto : collatedProtos) {
				piisPf.add(proto.project());
			}

			// il faut remplir les trous avec des périodes "SOURCE" sur toute la période (sauf en cas de décès)
			final RegDate dateDeces = tiersService.getDateDeces(pp);
			final RegDate dateFin = RegDateHelper.minimum(pfRange.getDateFin(), dateDeces, NullDateBehavior.LATEST);
			if (RegDateHelper.isBeforeOrEqual(pfRange.getDateDebut(), dateFin, NullDateBehavior.LATEST)) {
				final List<PeriodeImpositionImpotSource> fonds = new ArrayList<>(1);
				fonds.add(new PeriodeImpositionImpotSource(pp, pfRange.getDateDebut(), dateFin));

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

		return piis.isEmpty() ? Collections.emptyList() : collate(piis);
	}

	/**
	 * Comme les périodes "déteignent" sur les périodes <b>précédentes</b> en cas de collation, et que l'algorithme
	 * fourni par {@link DateRangeHelper#collate(java.util.List)} prend les éléments dans l'ordre canonique, il peut être nécessaire
	 * de faire plusieurs itérations de <i>collate</i> avant d'obtenir une situation stable
	 * @param src liste de base
	 * @return liste collatée (on arrête les boucles dès que le nombre d'éléments dans la liste ne bouge plus)
	 */
	private static <T extends CollatableDateRange> List<T> collate(@NotNull List<T> src) {
		List<T> collated = src;
		while (!collated.isEmpty()) {
			final int size = collated.size();
			collated = DateRangeHelper.collate(collated);
			if (size == collated.size()) {
				break;
			}
		}
		return collated;
	}
}
