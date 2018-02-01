package ch.vd.unireg.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;

class MultilineStringEditor implements Editor {

	private final EditorParams params;

	MultilineStringEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		final String v = value == null ? StringUtils.EMPTY : StringEscapeUtils.escapeHtml4(value);
		if (params.isReadonly()) {
			tagWriter.startTag("span");
			tagWriter.appendValue(v);
			tagWriter.endTag();
		}
		else {
			tagWriter.startTag("textarea");

			final String id = params.getId();
			if (StringUtils.isNotBlank(id)) {
				tagWriter.writeAttribute("id", id);
			}

			final String path = params.getPath();
			if (StringUtils.isNotBlank(path)) {
				tagWriter.writeAttribute("name", path);
			}

			// TODO à terme, il faudra peut-être rendre la taille de la section de texte configurable...
			tagWriter.writeAttribute("rows", Integer.toString(5));
			tagWriter.writeAttribute("cols", Integer.toString(40));
			tagWriter.writeAttribute("style", "resize:none");
			tagWriter.appendValue(v);
			tagWriter.endTag();
		}
	}
}
