package ch.vd.unireg.taglibs.formInput;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;

import ch.vd.unireg.supergra.EntityType;

class SuperGraEntityEditor implements Editor {

	private final EditorParams params;

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
			entityId = value.isEmpty() ? null : Long.parseLong(value);      // un null en base peut se transformer en chaine vide en html et en "boum" si on n'y prend pas garde
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
				tagWriter.writeAttribute("href", params.getContextPath() + "/supergra/entity/show.do?id=" + entityId + "&class=" + type);
				tagWriter.appendValue(type.getDisplayName() + " n°" + entityId);
				tagWriter.endTag();
			}
		}
		else {
			if (type == EntityType.Tiers || type == EntityType.AyantDroitRF || type == EntityType.ImmeubleRF || type == EntityType.BatimentRF || type == EntityType.Etiquette) {

				// lien vers l'entité
				tagWriter.startTag("a");
				tagWriter.writeAttribute("href-template", params.getContextPath() + "/supergra/entity/show.do?id=ENTITY_ID&class=" + type);
				if (entityId != null) {
					tagWriter.writeAttribute("href", params.getContextPath() + "/supergra/entity/show.do?id=" + entityId + "&class=" + type);
				}
				tagWriter.appendValue(type.getDisplayName() + " n°");
				tagWriter.endTag();

				// espace
				tagWriter.startTag("span");
				tagWriter.appendValue(" ");
				tagWriter.endTag();

				// champ d'édition
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
				tagWriter.writeAttribute("onchange", "updateEntityLink($(this).siblings('a'), $(this).val());");
				tagWriter.endTag();

				// bouton d'édition
				if (type == EntityType.Tiers) {
					tagWriter.startTag("button");
					tagWriter.writeAttribute("id", "button_" + id);
					tagWriter.writeAttribute("onclick", "return openTiersPicker(this, $(this).siblings('input'));");
					tagWriter.appendValue("...");
					tagWriter.endTag();
				}
			}
			else {
				// TODO (msi) gérer complétement le mode read-write sur les autres entités hibernate
				if (entityId == null) {
					// rien à faire
				}
				else {
					tagWriter.startTag("a");
					tagWriter.writeAttribute("href", params.getContextPath() + "/supergra/entity/show.do?id=" + entityId + "&class=" + type);
					tagWriter.appendValue(type.getDisplayName() + " n°" + entityId);
					tagWriter.endTag();
				}
			}

		}
	}
}
