package ch.vd.uniregctb.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.springframework.web.servlet.tags.form.TagWriter;

class DateEditor implements Editor {

	private EditorParams params;

	DateEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		if (params.isReadonly()) {
			tagWriter.startTag("span");
			tagWriter.appendValue(value);
			tagWriter.endTag();
		}
		else {

			tagWriter.startTag("input");
			final String id = params.getId();
			tagWriter.writeAttribute("id", id);
			tagWriter.writeAttribute("type", "text");
			tagWriter.writeAttribute("name", params.getPath());
			tagWriter.writeAttribute("value", value);
			tagWriter.endTag();

			final String script = "$(function() {\n" +
					"\t$( \"#" + id + "\" ).datepicker({" +
					"showOn: \"button\", " +
					"showAnim: '', " +
					"buttonImage: \"" + params.getContextPath() + "/css/x/calendar_off.gif\", " +
					"buttonImageOnly: true, " +
					"changeMonth: true, " +
					"changeYear: true});\n" +
					"});";

			tagWriter.startTag("script");
			tagWriter.appendValue(script);
			tagWriter.endTag();
		}
	}
}
