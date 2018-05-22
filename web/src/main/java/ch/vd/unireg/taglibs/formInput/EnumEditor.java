package ch.vd.unireg.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.springframework.web.servlet.tags.form.TagWriter;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

class EnumEditor implements Editor {

	private final EditorParams params;

	EnumEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		if (!params.getType().isEnum()) {
			throw new IllegalArgumentException();
		}

		final String v = value == null ? "" : value;
		if (params.isReadonly()) {
			tagWriter.startTag("span");
			tagWriter.appendValue(v);
			tagWriter.endTag();
		}
		else {
			final Object[] constants = params.getType().getEnumConstants();

			tagWriter.startTag("select");

			final String id = params.getId();
			if (isNotBlank(id)) {
				tagWriter.writeAttribute("id", id);
			}

			final String path = params.getPath();
			if (isNotBlank(path)) {
				tagWriter.writeAttribute("name", path);
			}

			tagWriter.startTag("option");
			tagWriter.endTag();


			for (Object c : constants) {
				tagWriter.startTag("option");
				tagWriter.writeAttribute("value", c.toString());
				if (c.toString().equals(value)) {
					tagWriter.writeAttribute("selected", "selected");
				}
				tagWriter.appendValue(c.toString());
				tagWriter.endTag();
			}

			tagWriter.endTag(); // select
		}
	}
}
