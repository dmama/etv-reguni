package ch.vd.uniregctb.common;

import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;

/**
 * classe qui permet de factoriser quelques petites méthodes utilitaires
 * autour des erreurs éditiques
 */
public abstract class EditiqueErrorHelper {

	private static String getComplementErreur(EditiqueResultat resultat) {
		if (resultat instanceof EditiqueResultatTimeout) {
			return "Time-out";
		}
		else if (resultat instanceof EditiqueResultatErreur) {
			return ((EditiqueResultatErreur) resultat).getError();
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
			builder.append(" (").append(complement).append(')');
		}
		builder.append('.');
		return builder.toString();
	}
}
