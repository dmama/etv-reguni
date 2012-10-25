package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Classe utilitaire pour le rendu Html dans Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HtmlHelper {

	/**
	 * Escape la chaîne de caractères spécifiée pour qu'elle puissent être rendue en Html sans effet secondaire. Les espaces, tabs et
	 * retours de lignes sont aussi convertis pour garder leurs effets.
	 *
	 * @param string
	 * @return
	 */
	public static String renderMultilines(String string) {
		string = StringEscapeUtils.escapeXml(string);
		string = string.replaceAll("\n", "<br/>");
		string = string.replaceAll(" ", "&nbsp;");
		string = string.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
		return string;
	}

}