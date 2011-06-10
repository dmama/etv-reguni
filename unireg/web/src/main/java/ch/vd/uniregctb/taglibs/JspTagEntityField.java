package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;

/**
 * Tag qui génère un champ d'édition pour une propriété de type 'lien vers une entité hibernate'.
 */
public class JspTagEntityField extends BodyTagSupport {

	private EntityType type;
	private Object value;
	private String path;
	private String id;
	private boolean readonly;

	@Override
	public int doStartTag() throws JspException {

		try {
			JspWriter out = pageContext.getOut();
			final String body = generate((HttpServletRequest) this.pageContext.getRequest());
			out.print(body);
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public String generate(HttpServletRequest request) {

		final String contextPath = request.getContextPath();
		final EntityKey entity = (EntityKey) value;
		final Long entityId = (entity == null ? null : entity.getId());
		if (type == null) {
			if (entityId == null) {
				return "";
			}
			else {
				return "(unknown) n°" + entityId;
			}
		}
		else if (readonly) {
			if (entityId == null) {
				return "";
			}
			else {
				return "<a href=\"" + contextPath + "/supergra/entity.do?id=" + entityId + "&class=" + type + "\"/>" + type.getDisplayName() + " n°" + entityId + "</a>";
			}
		}
		else {
			if (type == EntityType.Tiers) {
				final StringBuilder editor = new StringBuilder();
				editor.append("<input ");
				if (StringUtils.isNotBlank(id)) {
					editor.append("id=\"").append(id).append("\" ");
				}
				if (StringUtils.isNotBlank(path)) {
					editor.append("name=\"").append(path).append("\" ");
				}
				editor.append("type=\"text\" ");
				if (entityId != null) {
					editor.append("value=\"").append(entityId).append("\"");
				}
				editor.append("/>");
				final String input = editor.toString();

				final String button = "<button id=\"button_" + id + "\" onclick=\"return open_tiers_picker(this, function(id) {$('#" + id + "').val(id);});\">...</button>";
				return input + button;
			}
			else {
				// TODO (msi) gérer complétement le mode read-write sur les autres entités hibernate
				if (entityId == null) {
					return "";
				}
				else {
					return "<a href=\"" + contextPath + "/supergra/entity.do?id=" + entityId + "&class=" + type + "\"/>" + type.getDisplayName() + " n°" + entityId + "</a>";
				}
			}

		}
	}
}
