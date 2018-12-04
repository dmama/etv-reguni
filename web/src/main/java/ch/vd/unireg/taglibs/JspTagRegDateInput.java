package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspException;

import org.apache.taglibs.standard.tag.common.core.UrlSupport;
import org.springframework.web.servlet.tags.form.InputTag;
import org.springframework.web.servlet.tags.form.TagWriter;

/**
 * Form input spécialisé pour les RegDate (attention, il n'inclut pas les messages d'erreur : il faut les ajouter explicitement si nécessaire).
 */
public class JspTagRegDateInput extends InputTag {

	private static final long serialVersionUID = 6487059809150371291L;

	private boolean mandatory;

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {

		final String id = resolveId();

		// l'input de la date
		tagWriter.startTag("input");
		writeOptionalAttribute(tagWriter, "id", id);
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", "text");
		writeValue(tagWriter);
		writeOptionalAttribute(tagWriter, SIZE_ATTRIBUTE, "10");
		writeOptionalAttribute(tagWriter, MAXLENGTH_ATTRIBUTE, "10");
		writeOptionalAttribute(tagWriter, ALT_ATTRIBUTE, getAlt());
		tagWriter.endTag();

		if (mandatory) {
			tagWriter.startTag("span");
			writeOptionalAttribute(tagWriter, "class", "mandatory");
			tagWriter.writeAttribute("type", "hidden");
			tagWriter.writeAttribute("style", "padding-left: 5px");
			tagWriter.appendValue("*");
			tagWriter.endTag();
		}

		final String buttonImageUrl = UrlSupport.resolveUrl("/css/x/calendar_off.gif", null, pageContext);

		// le script pour activer le datepicker
		tagWriter.startTag("script");
		tagWriter.appendValue(
				"$(function() { $('#" + id + "').datepicker({" +
						"showOn: \"button\", " +
						"showAnim: '', " +
						"yearRange: '1900:+20', " +
						"buttonImage: \"" + buttonImageUrl + "\", " +
						"buttonImageOnly: true, " +
						"changeMonth: true, " +
						"changeYear: true}); " +
						"});");
		tagWriter.endTag();

		return SKIP_BODY;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
}
