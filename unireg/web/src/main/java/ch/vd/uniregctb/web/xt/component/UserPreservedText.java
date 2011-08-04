package ch.vd.uniregctb.web.xt.component;

import org.apache.commons.lang.StringEscapeUtils;
import org.springmodules.xt.ajax.component.Component;

/**
 * Encapsule un texte saisi par l'utilisateur. Ce texte sera escapé pour éviter toute injection de code HTML ou javascript.
 * <p/>
 * D'autre part, les espaces et les retours de lignes seront remplacés par leurs équivalents Html pour préserver le flux du texte autant que possible.
 *
 * @see http://issuetracker.etat-de-vaud.ch/jira/browse/SIFISC-1685
 */
public class UserPreservedText implements Component {

	private static final long serialVersionUID = 1L;

	private final String text;

	public UserPreservedText(String text) {
		this.text = text;
	}

	@Override
	public String render() {
		if (text != null) {
			final String escaped = StringEscapeUtils.escapeXml(this.text); // escape XML standard
			final String linebreaks = escaped.replaceAll("\\n", "<br/>"); // remplacement des retours de lignes par la balise XHTML correspondante
			return linebreaks.replaceAll("  ", "&#160; "); // remplacement des espaces multiples (sauf le dernier de chaque série) par des espaces insécables (&#160; = non-breaking space in XML)
		}
		else {
			return "";
		}
	}
}