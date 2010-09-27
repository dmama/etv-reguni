package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;

/**
 * Tag jsp qui permet de récupérer divers éléments du service infrastructure par id.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagFormField extends BodyTagSupport {

	private static final long serialVersionUID = -8958197495549589352L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	private Class clazz;
	private Object value;
	private String path;
	private String id;
	private boolean readonly;

	protected static final Map<Class, Editor> editors = new HashMap<Class, Editor>();

	static {
		editors.put(String.class, new StringEditor());
		editors.put(Long.class, new NumberEditor());
		editors.put(Integer.class, new NumberEditor());
		editors.put(Boolean.class, new BooleanEditor());
		editors.put(Date.class, new DateEditor());
		editors.put(RegDate.class, new DateEditor());
	}

	@Override
	public int doStartTag() throws JspException {

		Editor editor = getEditor(clazz);

		final String body = editor.generate(id, path, clazz, value, readonly, (HttpServletRequest) this.pageContext.getRequest());

		try {
			JspWriter out = pageContext.getOut();
			out.print(body);
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	private Editor getEditor(Class clazz) {
		Editor editor = null;
		if (this.clazz.isEnum()) {
			editor = new EnumEditor(); // cas spécial pour les enums
		}
		else {
			for (Map.Entry<Class, Editor> entry : editors.entrySet()) {
				final Class key = entry.getKey();
				if (key.isAssignableFrom(clazz)) {
					editor = entry.getValue();
					break;
				}
			}
		}
		if (editor == null) {
			editor = new StringEditor(); // éditeur par défaut
		}
		return editor;
	}

	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	protected static interface Editor {
		public abstract String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request);
	}

	private static class StringEditor implements Editor {
		public String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request) {

			final String v = value == null ? "" : StringEscapeUtils.escapeHtml(value.toString());
			if (readonly) {
				return v;
			}
			else {
				final StringBuilder editor = new StringBuilder();
				editor.append("<input ");

				if (StringUtils.isNotBlank(id)) {
					editor.append("id=\"").append(id).append("\" ");
				}
				if (StringUtils.isNotBlank(path)) {
					editor.append("name=\"").append(path).append("\" ");
				}

				editor.append("type=\"text\" value=\"").append(v).append("\"/>");

				return editor.toString();
			}
		}
	}

	private static class NumberEditor implements Editor {
		public String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request) {

			final String v = value == null ? "" : value.toString();
			if (readonly) {
				return v;
			}
			else {
				final StringBuilder editor = new StringBuilder();
				editor.append("<input ");

				if (StringUtils.isNotBlank(id)) {
					editor.append("id=\"").append(id).append("\" ");
				}
				if (StringUtils.isNotBlank(path)) {
					editor.append("name=\"").append(path).append("\" ");
				}

				editor.append("type=\"text\" value=\"").append(v).append("\"/>");

				return editor.toString();
			}
		}
	}

	private static class BooleanEditor implements Editor {
		public String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request) {

			final StringBuilder editor = new StringBuilder();
			editor.append("<input ");

			if (StringUtils.isNotBlank(id)) {
				editor.append("id=\"").append(id).append("\" ");
			}
			if (StringUtils.isNotBlank(path)) {
				editor.append("name=\"").append(path).append("\" ");
			}

			final boolean checked;
			if (value instanceof String) {
				final String s = (String) value;
				checked = (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on"));
			}
			else if (value instanceof Boolean) {
				checked = (Boolean) value;
			}
			else {
				checked = false;
			}

			editor.append("type=\"checkbox\" ");
			if (checked) {
				editor.append("checked=\"checked\" ");
			}

			if (readonly) {
				editor.append("disabled=\"true\" ");
			}

			editor.append("/>");

			return editor.toString();
		}
	}

	private static class DateEditor implements Editor {
		public String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request) {

			String displayDate;
			if (value instanceof RegDate) {
				final RegDate date = (RegDate) value;
				displayDate = RegDateHelper.dateToDisplayString(date);
			}
			else if (value instanceof Date) {
				final Date date = (Date) value;
				displayDate = DateHelper.dateToDisplayString(date);
			}
			else if (value instanceof String) {
				displayDate = (String) value; // on assume que la date est correctement formattée
			}
			else if (value == null) {
				displayDate = "";
			}
			else {
				throw new IllegalArgumentException("Unknown date class = [" + value.getClass() + "]");
			}

			final StringBuilder editor = new StringBuilder();

			if (readonly) {
				editor.append(displayDate);
			}
			else {
				final String line1 =
						String.format("<input type=\"text\" name=\"%s\" value=\"%s\" id=\"%s\" size=\"10\" maxlength =\"10\" class=\"date\"/>", path, displayDate, id);
				final String line2 =
						String.format("<a href=\"#\" name=\"%s_Anchor\" id=\"%s_Anchor\" tabindex=\"9999\" class=\"calendar\" onclick=\"calendar(E$('%s'), '%s_Anchor');\" >&nbsp;</a>",
								id, id, id, id);
				editor.append(line1);
				editor.append(line2);
			}

			return editor.toString();
		}
	}

	private static class EnumEditor implements Editor {

		public String generate(String id, String path, Class clazz, Object value, boolean readonly, HttpServletRequest request) {

			Assert.isTrue(clazz.isEnum());

			final String v = value == null ? "" : value.toString();
			if (readonly) {
				return v;
			}
			else {
				final Object[] constants = clazz.getEnumConstants();

				final StringBuilder editor = new StringBuilder();
				editor.append("<select ");

				if (StringUtils.isNotBlank(id)) {
					editor.append("id=\"").append(id).append("\" ");
				}
				if (StringUtils.isNotBlank(path)) {
					editor.append("name=\"").append(path).append("\" ");
				}

				editor.append(">\n");
				editor.append("<option/>\n");

				for (Object c : constants) {
					editor.append("<option value=\"").append(c).append("\"");
					if (c == value) {
						editor.append(" selected=\"selected\"");
					}
					editor.append(">").append(c).append("</option>\n");
				}

				editor.append("</select>");

				return editor.toString();
			}
		}
	}
}