package ch.vd.uniregctb.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Format Helper
 *
 */
public class FormatNumeroHelper {

	/**
	 * Transforme le numero CTB en numero formatte pour l'affichage *
	 *
	 * @param numero
	 * @return
	 *
	 */
	public static String numeroCTBToDisplay(Long numero) {

		String s = "";
		if (numero != null) {
			String sNumero = String.valueOf(numero);
			int size = sNumero.length();
			if (size <= 2) {
				s = sNumero;
			}
			else if ( size <= 5) {
				s = sNumero.substring(0, size -2) + '.' + sNumero.substring(size - 2);
			}
			else {
				s = sNumero.substring(0, size - 5) + '.' + sNumero.substring(size - 5, size - 2) + '.' + sNumero.substring(size - 2);
			}
		}

		return s;
	}

	/**
	 * Affiche le numero d invidu sous forme d un string
	 *
	 * @param numero
	 * @return
	 *
	 */
	public static String numeroIndividuToDisplay(Long numero) {
		String s = "";

		if (numero != null) {
			s = String.valueOf(numero);
		}

		return s;
	}

	/**
	 * Formattage d'un numéro IDE : CHE123456789 devient alors CHE-123.456.789
	 * @param numero numéro IDE sous sa forme compacte
	 * @return le numéro sous une forme agréable à l'oeil
	 */
	public static String formatNumIDE(String numero) {
		numero = removeSpaceAndDash(numero);
		if (numero != null && Pattern.matches("[A-Z]{3}[0-9]{9}", numero)) {
			return numero.substring(0, 3) + "-" + numero.substring(3, 6) + "." + numero.substring(6, 9) + "." + numero.substring(9);
		}
		return StringUtils.trimToEmpty(numero);
	}

	/**
	 * Formatte un numéro cantonal (e.g. '100565926' => '100-565-926') pour l'affichage aux utilisateurs
	 * @param numero un numéro cantonal déjà formatté ou non
	 * @return un numéro cantonal formatté
	 */
	public static String formatCantonalId(String numero) {
		numero = removeSpaceAndDash(numero);
		if (Pattern.matches("[0-9]{9}", numero)) {
			return numero.replaceAll("([0-9]{3})([0-9]{3})([0-9]{3})", "$1-$2-$3");
		}
		return numero;
	}

	/**
	 *
	 * Format Nouveau Numero AVS
	 *
	 * @param numero
	 *
	 */
	public static String formatNumAVS(String sNumero) {
		String rtr = "";
		sNumero = removeSpaceAndDash(sNumero);
		if (sNumero != null && Pattern.matches("[0-9]*", sNumero)) {
			rtr = sNumero;
			if (sNumero.length() == 13 && sNumero.startsWith("756")) {
				rtr = "756." + sNumero.substring(3, 7) + '.' + sNumero.substring(7, 11) + '.'
						+ sNumero.substring(11, sNumero.length());
			}

		}
		return rtr;
	}

	/**
	 *
	 * Format Ancien Numero AVS
	 *
	 * @param numero
	 */

	public static String formatAncienNumAVS(String sNumero) {
		String rtr = "";
		sNumero = removeSpaceAndDash(sNumero);

		if (sNumero != null && Pattern.matches("[0-9]*", sNumero)) {
			if (sNumero.length() == 8 || sNumero.length() == 11) {
				rtr = sNumero.substring(0, 3) + '.' + sNumero.substring(3, 5) + '.' + sNumero.substring(5, 8);
			}
			if (sNumero.length() == 11) {
				rtr += '.' + sNumero.substring(8, 11);
			}
		}
		return rtr;
	}

	/**
	 * Complète - si nécessaire - les anciens numéros AVS sur 8 positions pour atteindre 11 positions (voir UNIREG-605).
	 */
	public static String completeAncienNumAvs(String numero) {

		final String num = removeSpaceAndDash(numero);
		if (num.length() == 8) {
			if (numero.charAt(numero.length() - 1) == '.') {
				numero += "000";
			}
			else {
				numero += ".000";
			}
		}
		return numero;
	}

	/**
	 *
	 * Format Date
	 *
	 * @param date
	 *
	 */

	public static String formatDate(String date) {
		if (date == null) {
			return "";
		}
		int size = date.length();
		if (size == 8) {
			return (date.substring(6, 8) + '.' + date.substring(4, 6) + '.' + date.substring(0, 4));
		}
		if (size == 6) {
			return (date.substring(4, 6) + '.' + date.substring(0, 4));
		}
		Pattern p = Pattern.compile("^(\\d{4})\\.(\\d{2})\\.(\\d{2})$");
		Matcher m = p.matcher(date);
		if (m.matches()) {
			return (m.group(3) + '.' + m.group(2) + '.' + m.group(1));
		}
		return date;
	}

	public static String removeSpaceAndDash(String s) {

		if (s != null) {

			if (s.indexOf('.') != -1) {

				s = s.replace(".", "");
			}
			if (s.indexOf('-') != -1) {

				s = s.replace("-", "");
			}
			if (s.indexOf('/') != -1) {

				s = s.replace("/", "");
			}

			return s.replace(" ", "").trim();
		}
		else {
			return "";
		}
	}

	public static String extractNoReference(String ligneCodage) {

		int block =5 ;
		String noRef = "";
		String noRefFormate = "";
		noRef = StringUtils.substringAfter(ligneCodage, ">");
		if (StringUtils.indexOf(noRef, "+") < 0) {
			noRef = "";
		}
		if (noRef != null) {
			noRef = StringUtils.substringBefore(noRef, "+");
		}
		if (noRef != null) {
			noRef = StringUtils.reverse(noRef);
		}
		int tailleNoReference = noRef != null ? noRef.length() : 0;
		int nbreSeparateurs = tailleNoReference / block;
		for (int i=0; i<nbreSeparateurs; i++) {
			noRefFormate = noRefFormate + StringUtils.substring(noRef, 0, block) ;
			if (i < nbreSeparateurs -1) {
				noRefFormate = noRefFormate + ' ';
				noRef = StringUtils.substring(noRef, block);
			}
			else {
				noRef = StringUtils.substring(noRef, block, block + (tailleNoReference % block));
			}
		}
		if ((noRef != null) && (noRef.length() > 0)) {
			if (tailleNoReference > block) {
				noRefFormate = noRefFormate + ' ' + noRef;
			}
			else {
				noRefFormate = noRef;
			}
		}
		if (noRefFormate != null) {
			noRefFormate = StringUtils.reverse(noRefFormate);
		}

		if ("".equals(noRefFormate)) {
			noRefFormate = null;
		}

		return noRefFormate;

	}

}
