package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Quelques méthodes pratiques pour la manipulation de listes d'assujettissements
 */
public abstract class AssujettissementHelper {

	private static final DateRangeHelper.AdapterCallback<Assujettissement> ADAPTER = new DateRangeHelper.AdapterCallback<Assujettissement>() {
		@Override
		public Assujettissement adapt(Assujettissement assujettissement, RegDate debut, RegDate fin) {
			final RegDate debutEffectif;
			final MotifFor motifFracDebut;
			if (debut != null && assujettissement.getDateDebut() != debut) {
				debutEffectif = debut;
				motifFracDebut = null;
			}
			else {
				debutEffectif = assujettissement.getDateDebut();
				motifFracDebut = assujettissement.getMotifFractDebut();
			}

			final RegDate finEffective;
			final MotifFor motifFracFin;
			if (fin != null && assujettissement.getDateFin() != fin) {
				finEffective = fin;
				motifFracFin = null;
			}
			else {
				finEffective = assujettissement.getDateFin();
				motifFracFin = assujettissement.getMotifFractFin();
			}

			return assujettissement.duplicate(debutEffectif, finEffective, motifFracDebut, motifFracFin);
		}
	};

	/**
	 * Découpe les assujettissements spécifié année par année pour les années spécifiées
	 *
	 * @param list      les assujettissements à découper
	 * @param startYear l'année de départ (comprise)
	 * @param endYear   l'année de fin (comprise)
	 * @return une liste d'assujettissements découpés année par année
	 */
	public static List<Assujettissement> split(List<Assujettissement> list, int startYear, int endYear) {

		List<Assujettissement> splitted = new ArrayList<>();

		// split des assujettissement par années
		for (int year = startYear; year <= endYear; ++year) {
			final List<Assujettissement> extracted = extractYear(list, year);
			splitted.addAll(extracted);
		}

		return splitted;
	}

	/**
	 * Extrait la partie de l'assujettissement liée à l'année donnée
	 * @param list l'assujettissement "complet"
	 * @param annee l'année (civile) à considérer
	 * @return la liste des assujettissements de l'année considérée
	 */
	public static List<Assujettissement> extractYear(List<Assujettissement> list, int annee) {
		return extract(list, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
	}

	/**
	 * Extrait la partie de l'assujettissement liée à la période entre les dates de début et de fin
	 * @param list l'assujettissement "complet"
	 * @param dateDebut la date de début de la période à considérer
	 * @param dateFin la date de fin de la période à considérer
	 * @return la liste des assujettissements de la période
	 */
	public static List<Assujettissement> extract(List<Assujettissement> list, RegDate dateDebut, RegDate dateFin) {
		if (list == null) {
			return null;
		}
		return DateRangeHelper.extract(list, dateDebut, dateFin, ADAPTER);
	}

	/**
	 * @param source calculateur d'assujettissement source
	 * @param annee année civile de limitation de l'assujettissement
	 * @param <T> type de contribuable supporté par le calculateur
	 * @return calculateur qui limite l'assujettissement rendu par le calculateur source à l'année civile indiquée
	 */
	public static <T extends Contribuable> AssujettissementCalculator<T> yearLimiting(final AssujettissementCalculator<T> source, final int annee) {
		return new AssujettissementCalculator<T>() {
			@Override
			public List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				final List<Assujettissement> all = source.determine(ctb, fpt, noOfsCommunesVaudoises);
				if (all == null) {
					return null;
				}
				final List<Assujettissement> yearly = extractYear(all, annee);
				return yearly.isEmpty() ? null : yearly;
			}
		};
	}

	/**
	 * @param source calculateur d'assujettissement source
	 * @param range range de limitation de l'assujettissement (obligatoire si <i>collate</i> est faux)
	 * @param collate indique s'il faut fusionner les assujettissements adjacents dans le résultat fourni
	 * @param <T> type de contribuable traité par le calculateur
	 * @return calculateur qui limite l'assujettissement rendu par le calculateur source à la période indiquée
	 */
	public static <T extends Contribuable> AssujettissementCalculator<T> rangeLimiting(final AssujettissementCalculator<T> source, @Nullable final DateRange range, final boolean collate) {
		return new AssujettissementCalculator<T>() {
			@Override
			public List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				final List<Assujettissement> all = source.determine(ctb, fpt, noOfsCommunesVaudoises);
				if (all == null) {
					return null;
				}

				final List<Assujettissement> splittedCollated;
				if (!collate) {
					if (range == null) {
						throw new IllegalArgumentException("Le range doit être spécifié si collate=false");
					}
					splittedCollated = split(all, range.getDateDebut().year(), range.getDateFin().year());
				}
				else {
					splittedCollated = DateRangeHelper.collate(all);
				}

				final List<Assujettissement> ranged;
				if (range != null) {
					// Limitation des assujettissements au range demandé
					ranged = extract(splittedCollated, range.getDateDebut(), range.getDateFin());
				}
				else {
					ranged = splittedCollated;
				}

				return ranged.isEmpty() ? null : ranged;
			}
		};
	}

	/**
	 * @param one une date
	 * @param two une autre date
	 * @return si l'année de <i>two</i> est la suivante de celle de <i>one</i>
	 */
	public static boolean isYearSwitch(RegDate one, RegDate two) {
		return one != null && two != null && two.year() == one.year() + 1;
	}

	/**
	 * Asserte que les ranges ne se chevauchent pas.
	 * @param ranges les ranges à tester, supposés triés
	 * @throws AssujettissementException si deux ranges se chevauchent
	 */
	public static <T extends DateRange> void assertCoherenceRanges(List<T> ranges) throws AssujettissementException {
		final MovingWindow<T> wnd = new MovingWindow<>(ranges);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<T> snapshot = wnd.next();
			final T next = snapshot.getNext();
			if (next != null && DateRangeHelper.intersect(snapshot.getCurrent(), next)) {
				throw new AssujettissementException("Le range [" + snapshot.getCurrent() + "] entre en collision avec le suivant [" + next + ']');
			}
		}
	}

	/**
	 * @param tiers un tiers (a priori un contribuable...)
	 * @param date une date de référence
	 * @return <code>true</code> si le tiers possède un for principal hors Suisse à la date de référence
	 */
	public static boolean isForPrincipalHorsSuisse(Tiers tiers, RegDate date) {
		final ForFiscalPrincipal ffp = tiers.getForFiscalPrincipalAt(date);
		return ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
	}

	/**
	 * Retourne la date de fin de présence en suisse si le départ a eu lieu la même année que la date passé en paramètre
	 * @param tiers à analyser
	 * @param date de référence
	 * @return la date de fin de présence en suisse, Null si aucun départ HS dans l'année trouvé
	 */
	public static RegDate getDateFinPresenceSuisseDansAnnee(Tiers tiers, RegDate date) {
		Set<ForFiscal> fors = tiers.getForsFiscaux();
		for (ForFiscal f : fors) {
			final boolean isAfterDateReference = f.getDateDebut().isAfterOrEqual(date);
			final boolean isMemeAnnee = f.getDateDebut().year()== date.year();
			final boolean isHorsSuisse = f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
			if (f.isPrincipal() && isAfterDateReference && isHorsSuisse && isMemeAnnee) {

				return f.getDateDebut().getOneDayBefore();
			}
		}
		return null;
	}
}
