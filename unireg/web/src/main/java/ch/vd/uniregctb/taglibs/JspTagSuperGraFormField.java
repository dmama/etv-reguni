package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;

import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;

public class JspTagSuperGraFormField extends JspTagFormField {

	static {
		editors.put(EntityKey.class, new EntityKeyEditor());
	}

	private static class EntityKeyEditor implements Editor {
		public String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request) {

			final String contextPath = request.getContextPath();
			if (value == null) {
				return "";
			}

			final EntityKey entity = (EntityKey) value;
			final Long entityId = entity.getId();
			final EntityType type = entity.getType();
			if (type == null) {
				return "(unknown) n°" + entityId;
			}
			else if (readonly) {
				return type.getDisplayName() + " n°" + entityId;
			}
			else {
				// TODO (msi) gérer complétement le mode read-write sur les entités hibernate
				return "<a href=\"" + contextPath + "/supergra/entity.do?id=" + entityId + "&class=" + type + "\"/>" + type.getDisplayName() + " n°" + entityId + "</a>";
			}
		}
	}
}
