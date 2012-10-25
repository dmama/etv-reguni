package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.net.URL;
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
 * Tag jsp qui permet d'afficher diverse valeurs spécifiques à Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagOut extends BodyTagSupport {

	private static final long serialVersionUID = 1793304913541750349L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	private Class clazz;
	private Object value;
	private String id;

	protected static final Map<Class, Editor> editors = new HashMap<Class, Editor>();

	static {
		editors.put(String.class, new StringEditor());
		editors.put(Long.class, new NumberEditor());
		editors.put(Integer.class, new NumberEditor());
		editors.put(Boolean.class, new BooleanEditor());
		editors.put(Date.class, new DateEditor());
		editors.put(RegDate.class, new DateEditor());
		editors.put(URL.class, new URLEditor());
	}

	@Override
	public int doStartTag() throws JspException {

		Editor editor = getEditor(clazz);

		final String body = editor.generate(id, clazz, value, (HttpServletRequest) this.pageContext.getRequest());

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

	@Override
	public void setId(String id) {
		this.id = id;
	}

	protected static interface Editor {
		public abstract String generate(String id, Class clazz, Object value, HttpServletRequest request);
	}

	private static class StringEditor implements Editor {
		@Override
		public String generate(String id, Class clazz, Object value, HttpServletRequest request) {
			return value == null ? "" : StringEscapeUtils.escapeHtml(value.toString());
		}
	}

	private static class NumberEditor implements Editor {
		@Override
		public String generate(String id, Class clazz, Object value, HttpServletRequest request) {
			return value == null ? "" : value.toString();
		}
	}

	private static class BooleanEditor implements Editor {
		@Override
		public String generate(String id, Class clazz, Object value, HttpServletRequest request) {

			final StringBuilder editor = new StringBuilder();
			editor.append("<input ");

			if (StringUtils.isNotBlank(id)) {
				editor.append("id=\"").append(id).append("\" ");
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

			editor.append("disabled=\"true\" ");
			editor.append("value=\"true\"/>");

			return editor.toString();
		}
	}

	private static class DateEditor implements Editor {
		@Override
		public String generate(String id, Class clazz, Object value, HttpServletRequest request) {

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
				throw new IllegalArgumentException("Unknown date class = [" + value.getClass() + ']');
			}

			return displayDate;
		}
	}

	private static class EnumEditor implements Editor {

		@Override
		public String generate(String id, Class clazz, Object value, HttpServletRequest request) {
			Assert.isTrue(clazz.isEnum());
			return value == null ? "" : value.toString();
		}
	}

	private static class URLEditor implements Editor {
		@Override
		public String generate(String id, Class clazz, Object value, HttpServletRequest request) {
			return "<a href=\"" + value + "\">" + value + "</a>";
		}
	}
}