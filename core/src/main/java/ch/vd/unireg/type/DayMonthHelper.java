package ch.vd.unireg.type;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Quelques méthodes utilitaires autour des {@link DayMonth}
 */
public abstract class DayMonthHelper {

	private static final Pattern INDEX_PATTERN = Pattern.compile("([0-9]{1,2})([0-9]{2})");
	private static final Pattern DISPLAY_PATTERN = Pattern.compile("([0-9]{1,2})\\.([0-9]{1,2})");

	public enum StringFormat {

		/**
		 * MMDD
		 */
		INDEX {
			@Override
			public String toString(DayMonth dm) {
				if (dm == null) {
					return StringUtils.EMPTY;
				}
				return String.format("%02d%02d", dm.month(), dm.day());
			}

			@Override
			public DayMonth fromString(String string) throws ParseException {
				if (StringUtils.isBlank(string)) {
					return null;
				}
				final Matcher matcher = INDEX_PATTERN.matcher(string);
				if (matcher.matches()) {
					final int month = Integer.parseInt(matcher.group(1));
					final int day = Integer.parseInt(matcher.group(2));
					try {
						return DayMonth.get(month, day);
					}
					catch (IllegalArgumentException e) {
						throw new ParseException("La chaîne de caractères [" + string + "] ne représente pas un jour de l'année valide (" + e.getMessage() + ")", 0);
					}
				}
				else {
					throw new ParseException("La chaîne de caractères [" + string + "] ne représente pas un jour de l'année valide", 0);
				}
			}
		},

		/**
		 * DD.MM
		 */
		DISPLAY {
			@Override
			public String toString(DayMonth dm) {
				if (dm == null) {
					return StringUtils.EMPTY;
				}
				return String.format("%02d.%02d", dm.day(), dm.month());
			}

			@Override
			public DayMonth fromString(String string) throws ParseException {
				if (StringUtils.isBlank(string)) {
					return null;
				}
				final Matcher matcher = DISPLAY_PATTERN.matcher(string);
				if (matcher.matches()) {
					final int day = Integer.parseInt(matcher.group(1));
					final int month = Integer.parseInt(matcher.group(2));
					try {
						return DayMonth.get(month, day);
					}
					catch (IllegalArgumentException e) {
						throw new ParseException("La chaîne de caractères [" + string + "] ne représente pas un jour de l'année valide (" + e.getMessage() + ")", 0);
					}
				}
				else {
					throw new ParseException("La chaîne de caractères [" + string + "] ne représente pas un jour de l'année valide", 0);
				}
			}
		};

		/**
		 * Méthode pour générer une chaîne de caractères depuis un {@link DayMonth}
		 * @param dm le {@link DayMonth} en question
		 * @return la chaîne de caractères produite
		 */
		public abstract String toString(DayMonth dm);

		/**
		 * Méthode pour reconstruire une instance de {@link DayMonth} depuis une chaîne de caractères
		 * @param string la chaîne à parser
		 * @return l'instance de {@link DayMonth} reconstruite (<code>null</code> si la chaine est vide ou nulle)
		 * @throws ParseException en cas d'erreur de parsing
		 */
		public abstract DayMonth fromString(String string) throws ParseException;
	}

	/**
	 * @param dm {@link DayMonth} dont on veut une représentation "lisible" au format DD.MM
	 * @return la chaîne de caractères correspondante
	 */
	public static String toDisplayString(DayMonth dm) {
		return StringFormat.DISPLAY.toString(dm);
	}

	/**
	 * @param dm {@link DayMonth} dont on veut une représentation "lisible" au format DD.MM
	 * @return la chaîne de caractères correspondante
	 */
	public static String toIndexString(DayMonth dm) {
		return StringFormat.INDEX.toString(dm);
	}

	/**
	 * @param displayString une chaîne de caractères dont on pense qu'elle représente un {@link DayMonth} au format DD.MM
	 * @return l'instance de {@link DayMonth} correspondante à la chaîne de caractères en entrée
	 * @throws ParseException en cas de souci au parsing
	 */
	public static DayMonth fromDisplayString(String displayString) throws ParseException {
		return StringFormat.DISPLAY.fromString(displayString);
	}

	/**
	 * @param indexString une chaîne de caractères dont on pense qu'elle représente un {@link DayMonth} au format MMDD
	 * @return l'instance de {@link DayMonth} correspondante à la chaîne de caractères en entrée
	 * @throws ParseException en cas de souci au parsing
	 */
	public static DayMonth fromIndexString(String indexString) throws ParseException {
		return StringFormat.INDEX.fromString(indexString);
	}
}
