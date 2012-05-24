package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Classe utilitaire pour le rendu Html dans Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class HtmlHelper {

	/**
	 * Escape la chaîne de caractères spécifiée pour qu'elle puissent être rendue en Html sans effet secondaire. Les espaces, tabs et retours de lignes sont aussi convertis pour garder leurs effets.
	 *
	 * @param string une chaîne de caractères
	 * @return une autre chaîne de caractères; ou <b>null</b> si la chaînes passée en entrée est nulle elle-même.
	 */
	@Nullable
	public static String renderMultilines(@Nullable String string) {
		if (string == null) {
			return null;
		}
		string = StringEscapeUtils.escapeXml(string);
		string = string.replaceAll("\n", "<br/>");
		string = string.replaceAll(" ", "&nbsp;");
		string = string.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
		return string;
	}
}
