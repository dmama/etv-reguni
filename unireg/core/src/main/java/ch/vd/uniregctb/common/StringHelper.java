package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringUtils;

public class StringHelper {
	/**
	 * Complète la chaîne de caractères spécifiée avec des espaces au début de manière à ce qu'elle atteigne le longueur spécifiée.
	 *
	 * @param s   la chaîne de caractères à padder
	 * @param len la longueur désirée
	 * @return un chaîne de caractères de longueur minimale <i>len</i>.
	 */
	public static String lpad(String s, int len) {
		return lpad(s, len, ' ');
	}

	/**
	 * Complète la chaîne de caractères spécifiée avec des charactères au début de manière à ce qu'elle atteigne le longueur spécifiée.
	 *
	 * @param s       la chaîne de caractères à padder
	 * @param len     la longueur désirée
	 * @param padding le caractère utilisé comme padding
	 * @return un chaîne de caractères de longueur minimale <i>len</i>.
	 */
	public static String lpad(String s, int len, char padding) {
		final int l = s.length();
		if (l >= len) {
			return s;
		}
		return StringUtils.repeat(String.valueOf(padding), len - l) + s;
	}

	/**
	 * Complète la chaîne de caractères spécifiée avec des espaces à la fin de manière à ce qu'elle atteigne le longueur spécifiée.
	 *
	 * @param s   la chaîne de caractères à padder
	 * @param len la longueur désirée
	 * @return un chaîne de caractères de longueur minimale <i>len</i>.
	 */
	public static String rpad(String s, int len) {
		return rpad(s, len, ' ');
	}

	/**
	 * Complète la chaîne de caractères spécifiée avec des charactères à la fin de manière à ce qu'elle atteigne le longueur spécifiée.
	 *
	 * @param s       la chaîne de caractères à padder
	 * @param len     la longueur désirée
	 * @param padding le caractère utilisé comme padding
	 * @return un chaîne de caractères de longueur minimale <i>len</i>.
	 */
	public static String rpad(String s, int len, char padding) {
		final int l = s.length();
		if (l >= len) {
			return s;
		}
		return s + StringUtils.repeat(String.valueOf(padding), len - l);
	}
}
