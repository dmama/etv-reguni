package ch.vd.uniregctb.webservices.tiers2.impl;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.Range;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class RangeHelper {

	public static boolean isBeforeOrEqual(Date left, Date right) {
		if (left.year < right.year) {
			return true;
		}
		if (left.year > right.year) {
			return false;
		}
		if (left.month < right.month) {
			return true;
		}
		if (left.month > right.month) {
			return false;
		}
		return left.day <= right.day;
	}

	public static boolean isAfterOrEqual(Date left, Date right) {
		if (left.year > right.year) {
			return true;
		}
		if (left.year < right.year) {
			return false;
		}
		if (left.month > right.month) {
			return true;
		}
		if (left.month < right.month) {
			return false;
		}
		return left.day >= right.day;
	}

	public static boolean isDateInRange(Date date, Range range) {
		if (date == null) {
			return range.getDateFin() == null;
		}
		return (range.getDateDebut() == null || isAfterOrEqual(date, range.getDateDebut())) && (range.getDateFin() == null || isBeforeOrEqual(date, range.getDateFin()));
	}

	/**
	 * Détermine si deux ranges de dates s'intersectent. Les dates nulles sont évaluées comme :
	 * <ul>
	 * <li>pour les dates de début comme la nuit des temps (Big Bang)</li>
	 * <li>pour les dates de fin comme la fin des temps (Big Crunch)</li>
	 * </ul>
	 *
	 * @param premier
	 *            le premier range de dates
	 * @param second
	 *            le second range de dates
	 * @return <b>vrai</b> si les deux deux ranges s'intersectent, <b>faux</b> autrement.
	 */
	public static boolean intersect(Range premier, Range second) {

		final boolean debutPremierAvantFinSecond = (premier.getDateDebut() == null || second.getDateFin() == null || isBeforeOrEqual(premier.getDateDebut(), second.getDateFin()));
		final boolean finPremierApresDebutSecond = (premier.getDateFin() == null || second.getDateDebut() == null || isAfterOrEqual(premier.getDateFin(), second.getDateDebut()));

		return (debutPremierAvantFinSecond && finPremierApresDebutSecond);
	}

	public static <T extends Range> T getAt(List<T> ranges, @Nullable Date date) {
		T range = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (isDateInRange(date, r)) {
					range = r;
					break;
				}
			}
		}
		return range;
	}

	public static <T extends Range> List<T> getAllAt(List<T> ranges, Date date) {
		List<T> list = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (isDateInRange(date, r)) {
					if (list == null) {
						list = new ArrayList<T>();
					}
					list.add(r);
				}
			}
		}
		return list;
	}

	public static <T extends Range> List<T> getAllAt(List<T> ranges, Range periode) {
		List<T> list = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (RangeHelper.intersect(r, periode)) {
					if (list == null) {
						list = new ArrayList<T>();
					}
					list.add(r);
				}
			}
		}
		return list;
	}
}
