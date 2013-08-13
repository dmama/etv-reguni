package ch.vd.unireg.xml.common.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
		return left.compareTo(right) < 0;
	}

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est égale ou plus petite (= plus proche de l'aube des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isBeforeOrEqual(Date left, Date right) {
		return left.compareTo(right) <= 0;
	}

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est égale ou plus grande (= plus proche de la fin des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isAfterOrEqual(Date left, Date right) {
		return left.compareTo(right) >= 0;
	}

	/**
	 * @param left  une date non-nulle
	 * @param right une autre date non-nulle
	 * @return <b>vrai</b> si la date de gauche est strictement plus grande (= plus proche de la fin des temps) que la date de droite; <b>faux</b> dans tous les autres cas.
	 */
	public static boolean isAfter(Date left, Date right) {
		return left.compareTo(right) > 0;
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
			return range.getDateTo() == null;
		}
		return (range.getDateFrom() == null || isAfterOrEqual(date, range.getDateFrom())) && (range.getDateTo() == null || isBeforeOrEqual(date, range.getDateTo()));
	}

	/**
	 * Trouve et retourne la période valide à une date donnée dans une collection de périodes. Les périodes sont supposées ne pas se chevaucher.
	 * <p/>
	 * <b>Note :</b> les périodes annulées ne sont pas prises en compte (voir l'interface {@link Cancelable}).
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
				boolean canceled = (r instanceof Cancelable && ((Cancelable) r).getCancellationDate() != null);
				if (!canceled && isDateInRange(r, date)) {
					range = r;
					break;
				}
			}
		}
		return range;
	}

	/**
	 * Trouve et retourne toutes les périodes valides à une date donnée dans une collection de périodes. Les périodes de la collection peuvent se chevaucher.
	 * <p/>
	 * <b>Note :</b> les périodes annulées ne sont pas prises en compte (voir l'interface {@link Cancelable}).
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
				boolean canceled = (r instanceof Cancelable && ((Cancelable) r).getCancellationDate() != null);
				if (!canceled && isDateInRange(r, date)) {
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

		final boolean debutPremierAvantFinSecond = (premier.getDateFrom() == null || second.getDateTo() == null || isBeforeOrEqual(premier.getDateFrom(), second.getDateTo()));
		final boolean finPremierApresDebutSecond = (premier.getDateTo() == null || second.getDateFrom() == null || isAfterOrEqual(premier.getDateTo(), second.getDateFrom()));

		return (debutPremierAvantFinSecond && finPremierApresDebutSecond);
	}

	/**
	 * Compare la position de deux ranges de dates, d'abord en fonction de la date de début puis, à dates de début égales, en fonction de la date de fin
	 * (un range annulé sera systématiquement placé après un range non-annulé)
	 * @param left range 1
	 * @param right range 2
	 * @return 0 si les ranges sont équivallents, négatif si le range de gauche est avant le range de droite, et positif dans le cas contraire
	 */
	public static int compareTo(DateRange left, DateRange right) {
		final boolean canceledLeft = left instanceof Cancelable && ((Cancelable) left).getCancellationDate() != null;
		final boolean canceledRight = right instanceof Cancelable && ((Cancelable) right).getCancellationDate() != null;
		if (canceledLeft == canceledRight) {
			final boolean nullFromLeft = left.getDateFrom() == null;
			final boolean nullFromRight = right.getDateFrom() == null;
			if (nullFromLeft == nullFromRight) {
				final int compareFrom = nullFromLeft ? 0 : left.getDateFrom().compareTo(right.getDateFrom());
				if (compareFrom == 0) {
					final boolean nullToLeft = left.getDateTo() == null;
					final boolean nullToRight = right.getDateTo() == null;
					if (nullToLeft == nullToRight) {
						return nullToLeft ? 0 : left.getDateTo().compareTo(right.getDateTo());
					}
					else {
						// le range qui se termine au big-crunch est après
						return nullToLeft ? 1 : -1;
					}
				}
				else {
					return compareFrom;
				}
			}
			else {
				// le range qui commence au big-bang est avant
				return nullFromLeft ? -1 : 1;
			}
		}
		else {
			// les ranges annulés sont après
			return canceledLeft ? 1 : -1;
		}
	}
}
