package ch.vd.uniregctb.evenement.retourdi.pm;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.StringComparator;

/**
 * Classe utilitaire pour comparer des chaînes de caractères d'une manière uniforme
 */
public abstract class StringEqualityHelper {

	/**
	 * @param one une chaîne de caractères
	 * @param two une autre chaîne de caractères
	 * @return <code>true</code> si les deux chaînes doivent être considérées comme identiques
	 */
	public static boolean equals(String one, String two) {
		if (one == two) {
			return true;
		}
		if (one == null || two == null) {
			return false;
		}
		final String canonical1 = StringUtils.trimToEmpty(StringComparator.toLowerCaseWithoutAccent(one));
		final String canonical2 = StringUtils.trimToEmpty(StringComparator.toLowerCaseWithoutAccent(two));
		return canonical1.equals(canonical2);
	}

}
