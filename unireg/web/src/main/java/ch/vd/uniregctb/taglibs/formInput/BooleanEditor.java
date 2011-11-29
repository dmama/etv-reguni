package ch.vd.uniregctb.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;

class BooleanEditor implements Editor {

	private EditorParams params;

	BooleanEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		tagWriter.startTag("input");

		final String id = params.getId();
		if (StringUtils.isNotBlank(id)) {
			tagWriter.writeAttribute("id", id);
		}

		final String path = params.getPath();
		if (StringUtils.isNotBlank(path)) {
			tagWriter.writeAttribute("name", path);
		}

		final boolean checked = value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on"));

		tagWriter.writeAttribute("type", "checkbox");

		if (checked) {
			tagWriter.writeAttribute("checked", "checked");
		}

		if (params.isReadonly()) {
			tagWriter.writeAttribute("disabled", "true");
		}

		tagWriter.writeAttribute("value", "true");
		tagWriter.endTag();

		// [UNIREG-2962] on imprime un deuxième checkbox invisible pour que Spring puisse détecter la checkbox lorsqu'elle est n'est pas checkée
		// (voir http://static.springsource.org/spring/docs/1.1.5/api/org/springframework/web/bind/ServletRequestDataBinder.html#setFieldMarkerPrefix%28java.lang.String%29)
		tagWriter.startTag("input");
		tagWriter.writeAttribute("type", "hidden");
		if (StringUtils.isNotBlank(id)) {
			tagWriter.writeAttribute("id", '_' + id); // <--- le préfix '_' de l'id est important ici !
		}
		if (StringUtils.isNotBlank(path)) {
			tagWriter.writeAttribute("name", '_' + path); // <--- le préfix '_' du nom est important ici !
		}
		tagWriter.writeAttribute("value", "true");
		tagWriter.endTag();
	}
}
