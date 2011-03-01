package ch.vd.uniregctb.common;

/**
 * Méthodes utilitaires sur la gestion des temps
 */
public abstract class TimeHelper {

	/**
	 * Formatte une durée sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param milliseconds le nombre de milliseconds
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
	 * Formatte une durée sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param days    le nombre de jours
	 * @param hours   le nombre d'heures (0-23)
	 * @param minutes le nombre de minutes (0-59)
	 * @param seconds le nombre de secondes (0-59)
	 * @return une string représentant la durée sous forme humaine.
	 */
	public static String formatDuree(int days, int hours, int minutes, int seconds) {

		final StringBuilder s = new StringBuilder();
		if (days > 0) {
			s.append(days).append(' ').append(pluralize(days, "jour")).append(", ");
		}
		if (days > 0 || hours > 0) {
			s.append(hours).append(' ').append(pluralize(hours, "heure")).append(", ");
		}
		if (days > 0 || hours > 0 || minutes > 0) {
			s.append(minutes).append(' ').append(pluralize(minutes, "minute")).append(" et ");
		}
		s.append(seconds).append(' ').append(pluralize(seconds, "seconde"));

		return s.toString();
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
