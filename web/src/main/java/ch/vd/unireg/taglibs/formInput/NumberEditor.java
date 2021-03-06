package ch.vd.unireg.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;

class NumberEditor implements Editor {

	private final EditorParams params;

	NumberEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		final String v = value == null ? "" : value;
		if (params.isReadonly()) {
			tagWriter.startTag("span");
			tagWriter.appendValue(v);
			tagWriter.endTag();
		}
		else {
			tagWriter.startTag("input");

			final String id = params.getId();
			if (StringUtils.isNotBlank(id)) {
				tagWriter.writeAttribute("id", id);
			}

			final String path = params.getPath();
			if (StringUtils.isNotBlank(path)) {
				tagWriter.writeAttribute("name", path);
			}

			tagWriter.writeAttribute("type", "text");
			tagWriter.writeAttribute("value", v);
			tagWriter.endTag();
		}
	}
}
