package ch.vd.unireg.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe utilitaire qui est sert à manipuler une chaîne de caractère destinée à être utilisée comme donnée métier.
 */
public abstract class LiteralStringHelper {

	public static final char COMMA = ';';
	public static final String EMPTY = StringUtils.EMPTY;

	/**
	 * Supression des espaces excédentaires et remplacement de tous les caractères "blancs" par un espace
	 * @param ligne
	 * @return
	 */
	public static String stripExtraSpacesAndBlanks(String ligne) {
		return StringUtils.isBlank(ligne) ? EMPTY : ligne.replaceAll("[\\s\\u00a0]+", " ").trim();
	}
}
