package ch.vd.uniregctb.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class ValidateHelper {

	public static boolean isPositiveInteger(String s) {

		if (s == null || s.trim().length() == 0) {
			return false;
		}

		return Pattern.matches("[0-9]+", s);
	}

	public static boolean isNumber(String s) {

		if (s == null || s.trim().length() == 0) {
			return false;
		}

		//return Pattern.matches("[.-+]?[0-9]+(\\[0-9]+)", s);
		return Pattern.matches("[0-9.+\\s]*", s);
	}

	public static boolean isNumberTel(String s) {

		if (s == null || s.trim().length() == 0) {
			return false;
		}
		return Pattern.matches("[0-9.+/'\\s]*", s);
	}

	public static boolean isValidDate(String s) {

		if (s == null || s.trim().length() == 0) {
			return false;
		}

		return Pattern.matches("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}", s);
	}

	public static boolean isValidEmail(String s) {

		if (s == null || s.trim().length() == 0) {
			return false;
		}

		return Pattern.matches("\\S+@{1}?\\S+\\.\\w{2,}?", s); // lettres@lettres.lettres (expression regulière)
	}

	public static boolean isValidString(String s) {

		if (s == null || s.trim().length() == 0) {
			return false;
		}

		return Pattern.matches("[-a-zA-Zéàèöüäïëûîêâôç'.\\s]*", s);
	}


	public static boolean isValidNouveauNumeroAssureSocial(String s) {

		s = FormatNumeroHelper.removeSpaceAndDash(s);

		if (s == null || s.length() !=13) {
			return false;
		}

		//return Pattern.matches("756\\.[0-9]{4}\\.[0-9]{4}\\.[0-9]{2}", s);	// e.g. 756.1234.5678.90
		return Pattern.matches("756[0-9]*", s);
	}

	public static boolean isValidAncienNumeroAssureSocial(String s) {

		String str = FormatNumeroHelper.removeSpaceAndDash(s);

		if (str == null || str.length() != 8 || str.length() != 11) {
			return false;
		}

		//return Pattern.matches("[0-9]{3}\\.[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}", s); // e.g. 250.29.255.116
		return Pattern.matches("[0-9]*", str);
	}

	/**
	 * Ajoute la liste d'erreurs.
	 *
	 * @param sourceErrors
	 *            les messages d'erreurs à ajouter dans errors.
	 * @param errors
	 *            le receveur
	 */
	public static void rejectErrors(List<String> sourceErrors, Errors errors) {
		if (sourceErrors != null && sourceErrors.size() != 0) {
			for (String error : sourceErrors) {
				errors.reject("global.error.msg", error);
			}
		}
	}

	/**
	 * Formattage des erreurs pour l'affichage
	 *
	 * @param erreurs
	 * @return
	 */
	public static String formatAffichageErreur(List<String> erreurs) {
		String affErreur = "";
		for (String erreur : erreurs) {
			affErreur = affErreur + /*"<br>" +*/ erreur;
		}
		return affErreur;
	}
}
