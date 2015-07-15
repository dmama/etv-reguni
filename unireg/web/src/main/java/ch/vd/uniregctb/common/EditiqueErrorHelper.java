package ch.vd.uniregctb.common;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.editique.EditiqueResultatErreur;

/**
 * classe qui permet de factoriser quelques petites méthodes utilitaires
 * autour des erreurs éditiques
 */
public abstract class EditiqueErrorHelper {

	/**
	 * @param resultat
	 * @return Renvoie un message d'erreur "La communication avec l'éditique a échoué... avec éventuellement un message plus précis
	 */
	public static String getMessageErreurEditique(EditiqueResultatErreur resultat) {
		final StringBuilder builder = new StringBuilder();
		builder.append("La communication avec l'éditique a échoué");

		final String complement = resultat.getErrorMessage();
		if (StringUtils.isNotBlank(complement)) {
			builder.append(" (").append(complement).append(')');
		}
		builder.append('.');
		return builder.toString();
	}
}
