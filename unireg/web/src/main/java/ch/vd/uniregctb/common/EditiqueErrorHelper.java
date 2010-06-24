package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.editique.EditiqueResultat;

/**
 * classe qui permet de factoriser quelques petites méthodes utilitaires
 * autour des erreurs éditiques
 */
public class EditiqueErrorHelper {

	private static String getComplementErreur(EditiqueResultat resultat) {
		if (resultat == null) {
			return "Time-out";
		}
		else if (!StringUtils.isBlank(resultat.getError())) {
			return resultat.getError();
		}
		else {
			return null;
		}
	}

	/**
	 * @param resultat
	 * @return Renvoie un message d'erreur "La communication avec l'éditique a échoué... avec éventuellement un message plus précis
	 */
	public static String getMessageErreurEditique(EditiqueResultat resultat) {
		final StringBuilder builder = new StringBuilder();
		builder.append("La communication avec l'éditique a échoué");

		final String complement = getComplementErreur(resultat);
		if (complement != null) {
			builder.append(" (").append(complement).append(")");
		}
		builder.append(".");
		return builder.toString();
	}
}
