package ch.vd.unireg.webservices.tiers3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.uniregctb.webservices.tiers3.Date;

/**
 * Classe utilitaire qui expose des méthodes en relation avec les dates et les périodes.
 */
@SuppressWarnings({"UnusedDeclaration"})
public abstract class DateHelper {

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est strictement plus petite (= plus proche de l'aube des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isBefore(Date left, Date right) {
		if (left.getYear() < right.getYear()) {
			return true;
		}
		if (left.getYear() > right.getYear()) {
			return false;
		}
		if (left.getMonth() < right.getMonth()) {
			return true;
		}
		//noinspection SimplifiableIfStatement
		if (left.getMonth() > right.getMonth()) {
			return false;
		}
		return left.getDay() < right.getDay();
	}

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est égale ou plus petite (= plus proche de l'aube des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isBeforeOrEqual(Date left, Date right) {
		if (left.getYear() < right.getYear()) {
			return true;
		}
		if (left.getYear() > right.getYear()) {
			return false;
		}
		if (left.getMonth() < right.getMonth()) {
			return true;
		}
		//noinspection SimplifiableIfStatement
		if (left.getMonth() > right.getMonth()) {
			return false;
		}
		return left.getDay() <= right.getDay();
	}

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est égale ou plus grande (= plus proche de la fin des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isAfterOrEqual(Date left, Date right) {
		if (left.getYear() > right.getYear()) {
			return true;
		}
		if (left.getYear() < right.getYear()) {
			return false;
		}
		if (left.getMonth() > right.getMonth()) {
			return true;
		}
		//noinspection SimplifiableIfStatement
		if (left.getMonth() < right.getMonth()) {
			return false;
		}
		return left.getDay() >= right.getDay();
	}

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est strictement plus grande (= plus proche de la fin des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isAfter(Date left, Date right) {
		if (left.getYear() > right.getYear()) {
			return true;
		}
		if (left.getYear() < right.getYear()) {
			return false;
		}
		if (left.getMonth() > right.getMonth()) {
			return true;
		}
		//noinspection SimplifiableIfStatement
		if (left.getMonth() < right.getMonth()) {
			return false;
		}
		return left.getDay() > right.getDay();
	}

	/**
	 * Détermine si une date est comprise dans une période.
	 *
	 * @param range une période temporelle
	 * @param date  une date (qui peut être nulle, dans ce cas elle est interprétée comme la fin des temps)
	 * @return <b>vrai</b> si la date est comprise dans la période temporelle spécifiée; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isDateInRange(DateRange range, Date date) {
		if (date == null) {
			return range.getDateFin() == null;
		}
		return (range.getDateDebut() == null || isAfterOrEqual(date, range.getDateDebut())) && (range.getDateFin() == null || isBeforeOrEqual(date, range.getDateFin()));
	}

	/**
	 * Trouve et retourne la période valide à une date donnée dans une collection de périodes. Les périodes sont supposées ne pas se chevaucher.
	 *
	 * @param ranges une collection de périodes
	 * @param date   une date utilisée comme critère de recherche
	 * @param <T>    le type concret des périodes considérées
	 * @return la période valide à la date spécifiée; ou <b>null</b> si aucune période n'est valide à la date spécifiée.
	 */
	public static <T extends DateRange> T getAt(Collection<T> ranges, Date date) {
		T range = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (isDateInRange(r, date)) {
					range = r;
					break;
				}
			}
		}
		return range;
	}

	/**
	 * Trouve et retourne toutes les périodes valides à une date donnée dans une collection de périodes. Les périodes de la collection peuvent se chevaucher.
	 *
	 * @param ranges une collection de périodes
	 * @param date   une date utilisée comme critère de recherche
	 * @param <T>    le type concret des périodes considérées
	 * @return la période valide à la date spécifiée; ou <b>null</b> si aucune période n'est valide à la date spécifiée.
	 */
	public static <T extends DateRange> List<T> getAllAt(Collection<T> ranges, Date date) {
		List<T> list = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (isDateInRange(r, date)) {
					if (list == null) {
						list = new ArrayList<T>();
					}
					list.add(r);
				}
			}
		}
		return list;
	}

	/**
	 * Détermine si deux ranges de dates s'intersectent. Les dates nulles sont évaluées comme : <ul> <li>pour les dates de début comme la nuit des temps (Big Bang)</li> <li>pour les dates de fin comme la
	 * fin des temps (Big Crunch)</li> </ul>
	 *
	 * @param premier le premier range de dates
	 * @param second  le second range de dates
	 * @return <b>vrai</b> si les deux deux ranges s'intersectent, <b>faux</b> autrement.
	 */
	public static boolean intersect(DateRange premier, DateRange second) {

		final boolean debutPremierAvantFinSecond = (premier.getDateDebut() == null || second.getDateFin() == null || isBeforeOrEqual(premier.getDateDebut(), second.getDateFin()));
		final boolean finPremierApresDebutSecond = (premier.getDateFin() == null || second.getDateDebut() == null || isAfterOrEqual(premier.getDateFin(), second.getDateDebut()));

		return (debutPremierAvantFinSecond && finPremierApresDebutSecond);
	}
}
