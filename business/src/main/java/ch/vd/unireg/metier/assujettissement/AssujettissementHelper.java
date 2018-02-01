package ch.vd.uniregctb.metier.assujettissement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Quelques méthodes pratiques pour la manipulation de listes d'assujettissements
 */
public abstract class AssujettissementHelper {

	private static final DateRangeHelper.AdapterCallback<Assujettissement> ADAPTER = new DateRangeHelper.AdapterCallback<Assujettissement>() {
		@Override
		public Assujettissement adapt(Assujettissement assujettissement, RegDate debut, RegDate fin) {
			final RegDate debutEffectif;
			final MotifAssujettissement motifFracDebut;
			if (debut != null && assujettissement.getDateDebut() != debut) {
				debutEffectif = debut;
				motifFracDebut = null;
			}
			else {
				debutEffectif = assujettissement.getDateDebut();
				motifFracDebut = assujettissement.getMotifFractDebut();
			}

			final RegDate finEffective;
			final MotifAssujettissement motifFracFin;
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
	 * @param source      les assujettissements à découper (ils sont triés)
	 * @param splitters   dates de fin de périodes à respecter
	 * @return une liste d'assujettissements découpés en fonction des dates de découpe données
	 */
	public static List<Assujettissement> split(List<Assujettissement> source, Set<RegDate> splitters) {
		final NavigableSet<RegDate> sortedSplitters = new TreeSet<>(splitters);
		final List<Assujettissement> splitted = new LinkedList<>();
		for (Assujettissement a : source) {
			final List<Assujettissement> localSplitted = split(a, sortedSplitters);
			splitted.addAll(localSplitted);
		}
		return splitted;
	}

	@NotNull
	private static List<Assujettissement> split(Assujettissement assujettissement, NavigableSet<RegDate> splitters) {
		final List<Assujettissement> splitted = new LinkedList<>();
		final NavigableSet<RegDate> relevantSplitters;
		if (assujettissement.getDateFin() != null) {
			relevantSplitters = splitters.headSet(assujettissement.getDateFin(), false).tailSet(assujettissement.getDateDebut(), true);
		}
		else {
			relevantSplitters = splitters.tailSet(assujettissement.getDateDebut(), true);
		}
		RegDate cursor = assujettissement.getDateDebut();
		for (RegDate splitter : relevantSplitters) {
			final List<Assujettissement> extracted = extract(Collections.singletonList(assujettissement), cursor, splitter);
			splitted.addAll(extracted);
			cursor = splitter.getOneDayAfter();
		}
		splitted.addAll(extract(Collections.singletonList(assujettissement), cursor, assujettissement.getDateFin()));
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
	 * @param range (optionnel) range limitant l'assujettissement retourné
	 * @param <T> type de contribuable supporté par le calculateur
	 * @return calculateur qui limite l'assujettissement rendu par le calculateur source au range indiqué, après <string>collate</string> sur les assujettissements sources
	 */
	public static <T extends Contribuable> AssujettissementCalculator<T> collatedRangeLimiting(final AssujettissementCalculator<T> source, @Nullable final DateRange range) {
		return new AssujettissementCalculator<T>() {
			@Override
			public List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				final List<Assujettissement> all = source.determine(ctb, fpt, noOfsCommunesVaudoises);
				if (all == null) {
					return null;
				}

				final List<Assujettissement> collated = DateRangeHelper.collate(all);
				final List<Assujettissement> ranged;
				if (range != null) {
					ranged = extract(collated, range.getDateDebut(), range.getDateFin());
				}
				else {
					ranged = collated;
				}

				return ranged.isEmpty() ? null : ranged;
			}
		};
	}

	/**
	 * @param source calculateur d'assujettissement source
	 * @param splittingRanges ensemble de dates auxquelles les assujettissements doivent être coupées (= dates de fin de période)
	 * @param <T> type de contribuable traité par le calculateur
	 * @return calculateur qui limite l'assujettissement rendu par le calculateur source à la période indiquée
	 */
	public static <T extends Contribuable> AssujettissementCalculator<T> rangeLimiting(final AssujettissementCalculator<T> source, final List<DateRange> splittingRanges) {
		return new AssujettissementCalculator<T>() {
			@Override
			public List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				final List<Assujettissement> all = source.determine(ctb, fpt, noOfsCommunesVaudoises);
				if (all == null) {
					return null;
				}

				final List<Assujettissement> splitted = DateRangeHelper.extract(all, splittingRanges, ADAPTER);
				return splitted.isEmpty() ? null : splitted;
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
	 * @param ctb un contribuable
	 * @param date une date de référence
	 * @return <code>true</code> si le tiers possède un for principal hors Suisse à la date de référence
	 */
	public static boolean isForPrincipalHorsSuisse(Contribuable ctb, RegDate date) {
		final ForFiscalPrincipal ffp = ctb.getForFiscalPrincipalAt(date);
		return ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
	}
}
