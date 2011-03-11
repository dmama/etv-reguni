package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Méthodes utilitaires sur la gestion des temps
 */
public abstract class TimeHelper {

	private static final long UNE_MINUTE = TimeUnit.MINUTES.toMillis(1);
	private static final long UNE_HEURE = TimeUnit.HOURS.toMillis(1);
	private static final long UN_JOUR = TimeUnit.DAYS.toMillis(1);

	/**
	 * Formatte une durée sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param milliseconds le nombre de millisecondes
	 * @return une string représentant la durée sous forme humaine.
	 */
	public static String formatDuree(long milliseconds) {
		final int seconds = (int) ((milliseconds / 1000) % 60);
		final int minutes = (int) ((milliseconds / 1000) / 60) % 60;
		final int hours = (int) ((milliseconds / 1000) / 3600) % 24;
		final int days = (int) ((milliseconds / 1000) / (3600 * 24));

		return formatDuree(days, hours, minutes, seconds);
	}

	/**
	 * Formatte une durée sous la forme <i>1j 00h 23m 01s</i>.
	 *
	 * @param milliseconds le nombre de millisecondes
	 * @return une string représentant la durée sous forme presque humaine.
	 */
	public static String formatDureeShort(long milliseconds) {
		final int seconds = (int) ((milliseconds / 1000) % 60);
		final int minutes = (int) ((milliseconds / 1000) / 60) % 60;
		final int hours = (int) ((milliseconds / 1000) / 3600) % 24;
		final int days = (int) ((milliseconds / 1000) / (3600 * 24));

		return formatDureeShort(days, hours, minutes, seconds);
	}

	/**
	 * Formatte une durée de manière approximative:<br/>
	 * <ul>
	 *     <li>si la durée est plus longue que 5 jours, on ne voit que les jours</li>
	 *     <li>si la durée est entre 1 et 5 jours, on voit les jours et les heures</li>
	 *     <li>si la durée est entre 5 et 24 heures, on ne voit que les heures</li>
	 *     <li>si la durée est entre 1 et 5 heures, on voit les heures et les minutes</li>
	 *     <li>si la durée est entre 5 et 60 minutes, on ne voit que les minutes</li>
	 *     <li>si la durée est entre 1 et 5 minutes, on voit les minutes et les secondes</li>
	 *     <li>sinon, on ne voit que les secondes</li>
	 * </ul>
	 * @param milliseconds durée, exprimée en millisecondes
	 * @return une version lisible approximative de la durée
	 */
	public static String formatDureeArrondie(long milliseconds) {
		if (milliseconds < 5 * UNE_MINUTE) {
			return formatDuree(milliseconds);
		}
		else if (milliseconds < 5 * UNE_HEURE) {
			return formatDuree(arrondi(milliseconds, UNE_MINUTE));
		}
		else if (milliseconds < 5 * UN_JOUR) {
			return formatDuree(arrondi(milliseconds, UNE_HEURE));
		}
		else {
			return formatDuree(arrondi(milliseconds, UN_JOUR));
		}
	}

	protected static long arrondi(long valeur, long quanta) {
		if (quanta <= 0) {
			throw new IllegalArgumentException("La valeur du quanta doit être strictement positive!");
		}

		final long arrondi;
		final long floor = (valeur / quanta - (valeur < 0 ? 1 : 0)) * quanta;
		if (Math.abs(floor - valeur) >= quanta / 2) {
			arrondi = floor + quanta;
		}
		else {
			arrondi = floor;
		}
		return arrondi;
	}

	/**
	 * Formatte une durée sous la forme <i>1j 00h 23m 01s</i>.
	 *
	 * @param days    le nombre de jours
	 * @param hours   le nombre d'heures (0-23)
	 * @param minutes le nombre de minutes (0-59)
	 * @param seconds le nombre de secondes (0-59)
	 * @return une string représentant la durée sous forme presque humaine.
	 */
	protected static String formatDureeShort(int days, int hours, int minutes, int seconds) {
		final String str;
		if (days == 0) {
			if (hours == 0) {
				if (minutes == 0) {
					str = String.format("%ds", seconds);
				}
				else {
					str = String.format("%dm %02ds", minutes, seconds);
				}
			}
			else {
				str = String.format("%dh %02dm %02ds", hours, minutes, seconds);
			}
		}
		else {
			str = String.format("%dj %02dh %02dm %02ds", days, hours, minutes, seconds);
		}
		return str;
	}

	/**
	 * Formatte une durée sous la forme <i>1 jour, 12 heures, 23 minutes et 1 seconde</i>.
	 * On n'écrira jamais "0 xxxx" sauf dans le cas des secondes, si aucune autre unité n'est non-nulle.
	 *
	 * @param days    le nombre de jours
	 * @param hours   le nombre d'heures (0-23)
	 * @param minutes le nombre de minutes (0-59)
	 * @param seconds le nombre de secondes (0-59)
	 * @return une string représentant la durée sous forme humaine.
	 */
	protected static String formatDuree(int days, int hours, int minutes, int seconds) {
		final List<String> parts = new ArrayList<String>(4);
		if (days > 0) {
			parts.add(String.format("%d %s", days, pluralize(days, "jour")));
		}
		if (hours > 0) {
			parts.add(String.format("%s %s", hours, pluralize(hours, "heure")));
		}
		if (minutes > 0) {
			parts.add(String.format("%s %s", minutes, pluralize(minutes, "minute")));
		}
		if (seconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
			parts.add(String.format("%s %s", seconds, pluralize(seconds, "seconde")));
		}
		final int nbParts = parts.size();
		if (nbParts == 1) {
			return parts.get(0);
		}
		else {
			final StringBuilder b = new StringBuilder(parts.get(0));
			for (int i = 1 ; i < nbParts - 1 ; ++ i) {
				b.append(", ").append(parts.get(i));
			}
			if (nbParts > 1) {
				b.append(" et ").append(parts.get(nbParts - 1));
			}
			return b.toString();
		}
	}

	/**
	 * Implémentation très stupide de la méthode pluralize (inspirée de Ruby on Rails) qui ajoute un 's' à la fin du mot singulier lorsque count > 1.
	 *
	 * @param count    le nombre d'occurences
	 * @param singular la version au singulier du mot
	 * @return la version au singulier ou au pluriel du mot en fonction du nombre d'occurences.
	 */
	protected static String pluralize(int count, String singular) {
		if (count > 1) {
			return singular + 's';
		}
		else {
			return singular;
		}
	}

}
