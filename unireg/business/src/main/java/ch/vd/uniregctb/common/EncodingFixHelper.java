package ch.vd.uniregctb.common;

import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe utilitaire qui regroupe quelques primitives autour des corrections d'encodings
 */
public abstract class EncodingFixHelper {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Charset ISO = Charset.forName("ISO-8859-1");

	/**
	 * Corrige une chaîne de caractères mal encodée (= UTF-8 lu comme si c'était de l'ISO-8859-1)
	 * @param src la chaîne de caractères mal transcrite
	 * @return la même chaîne en mieux
	 */
	public static String fixFromIso(String src) {
		return convert(src, ISO, UTF8);
	}

	/**
	 * Encode une chaîne de caractères volontairement mal (= UTF-8 lu comme si c'était de l'ISO-8859-1)
	 * @param src la chaîne de caractères à transcrire
	 * @return la même chaîne en moins-bien
	 */
	public static String breakToIso(String src) {
		return convert(src, UTF8, ISO);
	}

	public static String convert(String src, Charset srcCharset, Charset destCharset) {
		if (StringUtils.isBlank(src)) {
			return src;
		}
		return new String(src.getBytes(srcCharset), destCharset);
	}
}
