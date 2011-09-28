package ch.vd.uniregctb.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;

import ch.vd.uniregctb.supergra.EntityType;

public class SuperGraEntityEditor implements Editor {

	private EditorParams params;

	public SuperGraEntityEditor(EditorParams params) {
		this.params = params;
	}

	@Override
	public void generate(TagWriter tagWriter, String value) throws JspException {

		final EntityType type = (EntityType) params.getCategorie();

		final Long entityId;
		if (value == null) {
			entityId = null;
		}
		else {
			// on ignore tous les caractères non-numériques
			value = value.replaceAll("[^\\d]", "");
			entityId = Long.parseLong(value);
		}

		if (type == null) {
			if (entityId == null) {
				// rien à faire
			}
			else {
				tagWriter.startTag("span");
				tagWriter.appendValue("(unknown) n°" + entityId);
				tagWriter.endTag();
			}
		}
		else if (params.isReadonly()) {
			if (entityId == null) {
				// rien à faire
			}
			else {
				tagWriter.startTag("a");
				tagWriter.writeAttribute("href", params.getContextPath() + "/supergra/entity.do?id=" + entityId + "&class=" + type);
				tagWriter.appendValue(type.getDisplayName() + " n°" + entityId);
				tagWriter.endTag();
			}
		}
		else {
			if (type == EntityType.Tiers) {

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

				if (entityId != null) {
					tagWriter.writeAttribute("value", entityId.toString());
				}

				tagWriter.endTag();

				tagWriter.startTag("button");
				tagWriter.writeAttribute("id", "button_" + id);
				tagWriter.writeAttribute("onclick", "return open_tiers_picker(this, function(id) {$('#" + id + "').val(id);});");
				tagWriter.appendValue("...");
				tagWriter.endTag();
			}
			else {
				// TODO (msi) gérer complétement le mode read-write sur les autres entités hibernate
				if (entityId == null) {
					// rien à faire
				}
				else {
					tagWriter.startTag("a");
					tagWriter.writeAttribute("href", params.getContextPath() + "/supergra/entity.do?id=" + entityId + "&class=" + type);
					tagWriter.appendValue(type.getDisplayName() + " n°" + entityId);
					tagWriter.endTag();
				}
			}

		}
	}
}
