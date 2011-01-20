package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.tags.form.AbstractHtmlInputElementTag;
import org.springframework.web.servlet.tags.form.TagWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;

/**
 * Tag qui génère un champ d'édition pour la propriété spécifiée dans un formulaire.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagFormInput extends AbstractHtmlInputElementTag {

	private static final long serialVersionUID = -6771881242633345495L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	private Class clazz;
	private boolean readonly;

	protected static final Map<Class, Class<? extends Editor>> editors = new HashMap<Class, Class<? extends Editor>>();

	static {
		editors.put(String.class, StringEditor.class);
		editors.put(Long.class, NumberEditor.class);
		editors.put(Integer.class, NumberEditor.class);
		editors.put(Boolean.class, BooleanEditor.class);
		editors.put(Date.class, DateEditor.class);
		editors.put(RegDate.class, DateEditor.class);
	}

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {

		// On recherche l'éditeur qui va bien
		final Editor editor = newEditorFor(clazz);

		// On va chercher la valeur à afficher (note : en cas d'erreur de validation, il s'agit de l'erreur erronée saisie par l'utilisateur)
		final String value = getDisplayString(getBoundValue(), getPropertyEditor());

		// On générate le Html qui va bien
		editor.generate(tagWriter, value);

		return SKIP_BODY;
	}

	private Editor newEditorFor(Class clazz) {
		Editor editor = null;
		if (this.clazz.isEnum()) {
			editor = new EnumEditor(); // cas spécial pour les enums
		}
		else {
			for (Map.Entry<Class, Class<? extends Editor>> entry : editors.entrySet()) {
				final Class key = entry.getKey();
				if (key.isAssignableFrom(clazz)) {
					Class<? extends Editor> editorClass = entry.getValue();
					editor = instanciateEditor(editorClass);
					break;
				}
			}
		}
		if (editor == null) {
			editor = new StringEditor(); // éditeur par défaut
		}
		return editor;
	}

	@SuppressWarnings({"unchecked"})
	private Editor instanciateEditor(Class<? extends Editor> editorClass) {
		final Editor editor;
		try {
			Constructor<? extends Editor> constructor = editorClass.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			editor = constructor.newInstance(this);
		}
		catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return editor;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	protected static interface Editor {
		public abstract void generate(TagWriter tagWriter, String value) throws JspException;
	}

	private class StringEditor implements Editor {
		public void generate(TagWriter tagWriter, String value) throws JspException {

			final String v = value == null ? "" : StringEscapeUtils.escapeHtml(value);
			if (readonly) {
				tagWriter.startTag("span");
				tagWriter.appendValue(v);
				tagWriter.endTag();
			}
			else {
				tagWriter.startTag("input");

				final String id = getId();
				if (StringUtils.isNotBlank(id)) {
					tagWriter.writeAttribute("id", id);
				}

				final String path = getPath();
				if (StringUtils.isNotBlank(path)) {
					tagWriter.writeAttribute("name", path);
				}

				tagWriter.writeAttribute("type", "text");
				tagWriter.writeAttribute("value", v);
				tagWriter.endTag();
			}
		}
	}

	private class NumberEditor implements Editor {
		public void generate(TagWriter tagWriter, String value) throws JspException {

			final String v = value == null ? "" : value;
			if (readonly) {
				tagWriter.startTag("span");
				tagWriter.appendValue(v);
				tagWriter.endTag();
			}
			else {
				tagWriter.startTag("input");

				final String id = getId();
				if (StringUtils.isNotBlank(id)) {
					tagWriter.writeAttribute("id", id);
				}

				final String path = getPath();
				if (StringUtils.isNotBlank(path)) {
					tagWriter.writeAttribute("name", path);
				}

				tagWriter.writeAttribute("type", "text");
				tagWriter.writeAttribute("value", v);
				tagWriter.endTag();
			}
		}
	}

	private class BooleanEditor implements Editor {
		public void generate(TagWriter tagWriter, String value) throws JspException {

			tagWriter.startTag("input");

			final String id = getId();
			if (StringUtils.isNotBlank(id)) {
				tagWriter.writeAttribute("id", id);
			}

			final String path = getPath();
			if (StringUtils.isNotBlank(path)) {
				tagWriter.writeAttribute("name", path);
			}

			final boolean checked = value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on"));

			tagWriter.writeAttribute("type", "checkbox");

			if (checked) {
				tagWriter.writeAttribute("checked", "checked");
			}

			if (readonly) {
				tagWriter.writeAttribute("disabled", "true");
			}

			tagWriter.writeAttribute("value", "true");
			tagWriter.endTag();

			// [UNIREG-2962] on imprime un deuxième checkbox invisible pour que Spring puisse détecter la checkbox lorsqu'elle est n'est pas checkée
			// (voir http://static.springsource.org/spring/docs/1.1.5/api/org/springframework/web/bind/ServletRequestDataBinder.html#setFieldMarkerPrefix%28java.lang.String%29)
			tagWriter.startTag("input");
			tagWriter.writeAttribute("type", "hidden");
			if (StringUtils.isNotBlank(id)) {
				tagWriter.writeAttribute("id", "_" + id); // <--- le préfix '_' de l'id est important ici !
			}
			if (StringUtils.isNotBlank(path)) {
				tagWriter.writeAttribute("name", "_" + path); // <--- le préfix '_' du nom est important ici !
			}
			tagWriter.writeAttribute("value", "true");
			tagWriter.endTag();
		}
	}

	private class DateEditor implements Editor {
		public void generate(TagWriter tagWriter, String value) throws JspException {

			if (readonly) {
				tagWriter.startTag("span");
				tagWriter.appendValue(value);
				tagWriter.endTag();
			}
			else {

				tagWriter.startTag("input");
				final String id = getId();
				tagWriter.writeAttribute("id", id);
				tagWriter.writeAttribute("type", "text");
				tagWriter.writeAttribute("name", getPath());
				tagWriter.writeAttribute("value", value);
				tagWriter.endTag();

				final String script = "$(function() {\n" +
						"\t$( \"#" + id + "\" ).datepicker({" +
						"showOn: \"button\", " +
						"buttonImage: \"" + getContextPath() + "/css/x/calendar_off.gif\", " +
						"buttonImageOnly: true, " +
						"changeMonth: true, " +
						"changeYear: true});\n" +
						"});";

				tagWriter.startTag("script");
				tagWriter.appendValue(script);
				tagWriter.endTag();
			}
		}
	}

	private class EnumEditor implements Editor {

		public void generate(TagWriter tagWriter, String value) throws JspException {

			Assert.isTrue(clazz.isEnum());

			final String v = value == null ? "" : value;
			if (readonly) {
				tagWriter.startTag("span");
				tagWriter.appendValue(v);
				tagWriter.endTag();
			}
			else {
				final Object[] constants = clazz.getEnumConstants();

				tagWriter.startTag("select");

				final String id = getId();
				if (StringUtils.isNotBlank(id)) {
					tagWriter.writeAttribute("id", id);
				}

				final String path = getPath();
				if (StringUtils.isNotBlank(path)) {
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

	private String getContextPath() {
		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		return request.getContextPath();
	}
}
