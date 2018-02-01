package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.annonceIDE.AdresseAnnonceIDEView;

/**
 * Tag JSP qui permet d'afficher une adresse de demande d'annonce à l'IDE sur plusieurs lignes
 */
public class JspTagAdresseAnnonce extends BodyTagSupport {

	private AdresseAnnonceIDEView adresse;

	public void setAdresse(AdresseAnnonceIDEView adresse) {
		this.adresse = adresse;
	}

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml());
			return SKIP_BODY;
		}
		catch (Exception e) {
			throw new JspTagException(e);
		}
	}

	String buildHtml() {

		if (adresse == null) {
			return "";
		}

		final StringBuilder b = new StringBuilder();

		appendLine(b, adresse.getRue(), adresse.getNumero());
		if (StringUtils.isNotBlank(adresse.getNumeroAppartement())) {
			appendLine(b, "App: " + adresse.getNumeroAppartement());
		}
		appendLine(b, adresse.getTexteCasePostale(), adresse.getNumeroCasePostale());
		appendLine(b, adresse.getNpa(), adresse.getVille());
		if (adresse.getPays() != null) {
			appendLine(b, adresse.getPays().getNomCourt());
		}

		return b.toString();
	}

	private static void appendLine(@NotNull StringBuilder b, @Nullable String value) {
		final boolean first = (b.length() == 0);
		if (StringUtils.isNotBlank(value)) {
			if (!first) {
				b.append("<br>");
			}
			b.append(HtmlUtils.htmlEscape(value));
		}
	}

	private static void appendLine(@NotNull StringBuilder b, @Nullable Object... values) {

		if (values == null) {
			// rien à faire
			return;
		}

		final StringBuilder sub = new StringBuilder();
		boolean addedSomething = false;
		for (Object value : values) {
			if (value instanceof String) {
				if (StringUtils.isNotBlank((String) value)) {
					if (addedSomething) {
						sub.append(" ");
					}
					sub.append(HtmlUtils.htmlEscape((String) value));
					addedSomething = true;
				}
			}
			else if (value != null) {
				if (addedSomething) {
					sub.append(" ");
				}
				sub.append(HtmlUtils.htmlEscape(value.toString()));
				addedSomething = true;
			}
		}

		if (addedSomething) {
			final boolean first = (b.length() == 0);
			if (!first) {
				b.append("<br>");
			}
			b.append(sub);
		}
	}
}
