package ch.vd.unireg.type;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.regex.Pattern;

import gnu.trove.TIntObjectHashMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * Donnée représentant un couple jour/mois, sans donnée d'année (25 décembre, 1er janvier...)
 */
public final class DayMonth implements Serializable, Comparable<DayMonth> {

	private static final long serialVersionUID = 7894005325726916550L;
	public static final Pattern STRING_PATTERN = Pattern.compile("\\d\\d\\d\\d");

	/**
	 * Jour dans le mois (1..31)
	 */
	private final int day;

	/**
	 * Mois dans l'année (1..12)
	 */
	private final int month;

	/**
	 * La taille maximale de chaque mois...
	 */
	private static final int[] ALLOWED_MONTH_LENGTHS = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	/**
	 * Le dernier jour de chaque mois (si le jour donné est supérieur ou égal à cette valeur, c'est une fin de mois, pour gérer le mois de février)
	 * @see #isEndOfMonth()
	 */
	private static final int[] MONTH_LAST_DAY = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	/**
	 * Cache de toutes les valeurs précédemment allouées depuis le démarrage de l'application
	 */
	private static final TIntObjectHashMap map = new TIntObjectHashMap(366);

	/**
	 * Constructeur interne
	 * @param month le mois (1..12)
	 * @param day le jour (1..31)
	 */
	private DayMonth(int month, int day) {
		checkValidity(month, day);
		this.month = month;
		this.day = day;
	}

	/**
	 * @param month le mois (1..12)
	 * @param day le jour (1..31)
	 * @throws java.lang.IllegalArgumentException si les données ne sont pas dans leur plage de validité
	 */
	private static void checkValidity(int month, int day) throws IllegalArgumentException {
		if (month < 1 || month > 12) {
			throw new IllegalArgumentException("Month should be between 1 and 12 (found " + month + ").");
		}
		if (day < 1 || day > ALLOWED_MONTH_LENGTHS[month - 1]) {
			throw new IllegalArgumentException("Day should be between 1 and " + ALLOWED_MONTH_LENGTHS[month - 1] + " in month " + month + " (found " + day + ").");
		}
	}

	/**
	 * @param month le mois (1..12)
	 * @param day le jour (1..31)
	 * @return l'index entier (MMDD) à partir du jour et du mois
	 */
	static int index(int month, int day) {
		checkValidity(month, day);
		return month * 100 + day;
	}

	/**
	 * @return l'index entier (MMDD) du DayMonth
	 */
	public int index() {
		return index(month, day);
	}

	/**
	 * @return la valeur de DayMonth pour le jour courant
	 */
	public static DayMonth get() {
		final RegDate today = RegDate.get();
		return _get(today.month(), today.day());
	}

	/**
	 * @param month le mois (1..12)
	 * @param day le jour (1..31)
	 * @return la valeur de DayMonth correspondante
	 * @throws java.lang.IllegalArgumentException si les données ne sont pas dans leur plage de validité
	 */
	public static DayMonth get(int month, int day) {
		return _get(month, day);
	}

	/**
	 * @param date date de référence
	 * @return DayMonth correspondant
	 * @throws java.lang.IllegalArgumentException en cas de date partielle
	 */
	public static DayMonth get(RegDate date) {
		if (date.isPartial()) {
			throw new IllegalArgumentException("Date partielle non acceptée.");
		}
		return _get(date.month(), date.day());
	}

	/**
	 * @param index index entier (MMDD)
	 * @return valeur correspondante
	 * @throws java.lang.IllegalArgumentException si l'index n'est pas valide
	 */
	public static DayMonth fromIndex(int index) {
		final int month = index / 100;
		final int day = index % 100;
		return _get(month, day);
	}

	/**
	 * @param string une string (mm.dd)
	 * @return valeur correspondante
	 * @throws java.lang.IllegalArgumentException si l'index n'est pas valide
	 */
	public static DayMonth fromString(String string) {

		final String trimmed = StringUtils.trimToEmpty(string);
		if (!STRING_PATTERN.matcher(trimmed).matches()) {
			throw new IllegalArgumentException("La string [" + string + "] n'est pas valide");
		}

		final int month = Integer.parseInt(trimmed.substring(0, 2));
		final int day = Integer.parseInt(trimmed.substring(2));

		return _get(month, day);
	}

	/**
	 * @param month le mois (1..12)
	 * @param day le jour (1..31)
	 * @return la valeur de DayMonth correspondante, en ré-utilisant au besoin les valeurs déja calculées précédemment
	 * @throws java.lang.IllegalArgumentException si les données ne sont pas dans leur plage de validité
	 */
	private static DayMonth _get(int month, int day) throws IllegalArgumentException {
		final int index = index(month, day);
		DayMonth value;
		synchronized (map) {
			value = (DayMonth) map.get(index);
			if (value == null) {
				value = new DayMonth(month, day);
				map.put(index, value);
			}
		}
		return value;
	}

	@Override
	public int hashCode() {
		return index();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		return String.format("%02d%02d", month, day);
	}

	@Override
	public int compareTo(@NotNull DayMonth o) {
		return Integer.compare(index(), o.index());
	}

	/**
	 * @return la valeur du mois (1..12)
	 */
	public int month() {
		return month;
	}

	/**
	 * @return la valeur du jour (1..31)
	 */
	public int day() {
		return day;
	}

	/**
	 * Cette méthode permet de retourner l'instance <i>singleton</i> correcte du DayMonth dans le cadre de la sérialisation Java.
	 *
	 * @return l'instance <i>singleton</i> correspondant au mois et au jour désérialisés.
	 * @throws java.io.ObjectStreamException si ça foire.
	 * @see java.io.Serializable
	 */
	private Object readResolve() throws ObjectStreamException {
		return _get(month, day);
	}

	/**
	 * @return <code>true</code> si le DayMonth est une fin de mois
	 */
	public boolean isEndOfMonth() {
		// >= au lieu de == à cause du 28/29 février (les deux jours sont considérés comme des fins de mois)
		return day >= MONTH_LAST_DAY[month - 1];
	}

	/**
	 * @param origin date de départ
	 * @return la prochaine date (égale ou ultérieure à la date d'origine) qui correspond au DayMonth
	 */
	@NotNull
	public RegDate nextAfterOrEqual(@NotNull RegDate origin) {
		return next(origin, true);
	}

	@NotNull
	public RegDate nextAfter(@NotNull RegDate origin) {
		return next(origin, false);
	}

	@NotNull
	private RegDate next(@NotNull RegDate origin, boolean equalOk) {
		final int annee;
		if (origin.isPartial()) {
			// résumons :
			// - si seule l'année est connue : en fonction du flag equalOk, on prend l'année ou la suivante
			// - si seul le jour est inconnu, il faut regarder le mois en plus pour faire cette distinction
			if (origin.month() == RegDate.UNDEFINED) {
				annee = equalOk ? origin.year() : origin.year() + 1;
			}
			else {
				annee = (origin.month() < month || (equalOk && origin.month() == month)) ? origin.year() : origin.year() + 1;
			}
		}
		else {
			// ici, c'est facile, si on a déjà passé le DayMonth l'année de l'origine, ce sera l'année prochaine, sinon c'est encore la même année
			final DayMonth dmOrigin = get(origin);
			final int compareTo = compareTo(dmOrigin);
			final boolean memeAnnee;
			if (dmOrigin.isEndOfMonth() && isEndOfMonth() && dmOrigin.month == month) {
				memeAnnee = equalOk;
			}
			else {
				memeAnnee = compareTo >= (equalOk ? 0 : 1);
			}

			annee = memeAnnee ? origin.year() : origin.year() + 1;
		}

		// il y a juste un truc pour le 29 février... il ne faudrait pas que le prochain "29 février" soit dans 3 ans et demi...
		// on va donc dire que si le DayMonth représente une fin de mois, alors il faut pousser à la fin du mois...
		if (isEndOfMonth()) {
			return RegDate.get(annee, month, 1).getLastDayOfTheMonth();
		}
		else {
			return RegDate.get(annee, month, day);
		}
	}

	@NotNull
	public RegDate previousBefore(@NotNull RegDate origin) {
		return previous(origin, false);
	}

	@NotNull
	public RegDate previousBeforeOrEqual(@NotNull RegDate origin) {
		return previous(origin, true);
	}

	@NotNull
	private RegDate previous(@NotNull RegDate origin, boolean equalOk) {
		final int annee;
		if (origin.isPartial()) {
			// résumons :
			// - si seule l'année est connue : en fonction du flag equalOk, on prend l'année ou la précédente
			// - si seul le jour est inconnu, il faut regarder le mois en plus pour faire cette distinction
			if (origin.month() == RegDate.UNDEFINED) {
				annee = equalOk ? origin.year() : origin.year() - 1;
			}
			else {
				annee = (origin.month() > month || (equalOk && origin.month() == month)) ? origin.year() : origin.year() - 1;
			}
		}
		else {
			// ici, c'est facile, si on a déjà passé le DayMonth l'année de l'origine, ce sera l'année prochaine, sinon c'est encore la même année
			final DayMonth dmOrigin = get(origin);
			final int compareTo = compareTo(dmOrigin);
			final boolean memeAnnee;
			if (dmOrigin.isEndOfMonth() && isEndOfMonth() && dmOrigin.month == month) {
				memeAnnee = equalOk;
			}
			else {
				memeAnnee = compareTo <= (equalOk ? 0 : -1);
			}

			annee = memeAnnee ? origin.year() : origin.year() - 1;
		}

		// il y a juste un truc pour le 29 février... il ne faudrait pas que le prochain "29 février" soit dans 3 ans et demi...
		// on va donc dire que si le DayMonth représente une fin de mois, alors il faut pousser à la fin du mois...
		if (isEndOfMonth()) {
			return RegDate.get(annee, month, 1).getLastDayOfTheMonth();
		}
		else {
			return RegDate.get(annee, month, day);
		}
	}

	/**
	 * @return <code>true</code> si les deux {@link ch.vd.unireg.type.DayMonth} passés en paramètres peuvent être considérés comme pointant vers le même jour du mois
	 * (= il y a un nombre de mois entier entre deux dates sur ces ancres)
	 */
	public static boolean isSameDayOfMonth(DayMonth dm1, DayMonth dm2) {
		return dm1.day() == dm2.day() || (dm1.isEndOfMonth() && dm2.isEndOfMonth());
	}
}
