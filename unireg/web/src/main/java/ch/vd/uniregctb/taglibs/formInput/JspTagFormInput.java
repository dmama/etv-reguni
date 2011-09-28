package ch.vd.uniregctb.taglibs.formInput;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.tags.form.AbstractHtmlInputElementTag;
import org.springframework.web.servlet.tags.form.TagWriter;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.supergra.EntityType;

/**
 * Tag qui génère un champ d'édition pour la propriété spécifiée dans un formulaire.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagFormInput extends AbstractHtmlInputElementTag {

	private static final long serialVersionUID = -6771881242633345495L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	/**
	 * Le type de la valeur saisie
	 */
	private Class type;

	/**
	 * (Optionnel) catégorie permettant d'instancier un éditeur spécifique.
	 */
	private Object category;

	private boolean readonly;

	protected static final Map<Class, Class<? extends Editor>> commonEditors = new HashMap<Class, Class<? extends Editor>>();
	protected static final Map<Object, Class<? extends Editor>> specificEditors = new HashMap<Object, Class<? extends Editor>>();

	static {
		// les types de base
		commonEditors.put(String.class, StringEditor.class);
		commonEditors.put(Long.class, NumberEditor.class);
		commonEditors.put(Integer.class, NumberEditor.class);
		commonEditors.put(Boolean.class, BooleanEditor.class);
		commonEditors.put(Date.class, DateEditor.class);
		commonEditors.put(RegDate.class, DateEditor.class);

		// les types SuperGra
		for (EntityType type : EntityType.values()) {
			specificEditors.put(type, SuperGraEntityEditor.class);
		}
	}

	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {

		// On recherche l'éditeur qui va bien
		final Editor editor = newEditorFor(getPath(), type, category);

		// On va chercher la valeur à afficher (note : en cas d'erreur de validation, il s'agit de l'erreur erronée saisie par l'utilisateur)
		final Object value = getBoundValue();
		final String displayString = getDisplayString(value, getPropertyEditor());

		// On générate le Html qui va bien
		editor.generate(tagWriter, displayString);

		return SKIP_BODY;
	}

	/**
	 * Recherche et instancie l'éditeur qui correspond à la classe et à la catégorie spécifiée.
	 *
	 * @param path      le chemin vers la valeur saisie
	 * @param type      le type de valeur saisie
	 * @param categorie la catégorie de valeur saisie (optionnel)
	 * @return un éditeur
	 */
	@NotNull
	private Editor newEditorFor(String path, Class type, @Nullable Object categorie) {

		Editor editor = null;

		final EditorParams params = new EditorParams(getId(), path, type, categorie, readonly, getContextPath());

		if (categorie != null) {
			Class<? extends Editor> editorClass = specificEditors.get(categorie);
			editor = instanciateEditor(editorClass, params);
		}

		if (editor == null) {
			if (type.isEnum()) {
				editor = new EnumEditor(params); // cas spécial pour les enums
			}
		}

		if (editor == null) {
			for (Map.Entry<Class, Class<? extends Editor>> entry : commonEditors.entrySet()) {
				final Class key = entry.getKey();
				if (key.isAssignableFrom(type)) {
					Class<? extends Editor> editorClass = entry.getValue();
					editor = instanciateEditor(editorClass, params);
					break;
				}
			}
		}

		if (editor == null) {
			editor = new StringEditor(params); // éditeur par défaut
		}

		return editor;
	}

	@SuppressWarnings({"unchecked"})
	private Editor instanciateEditor(Class<? extends Editor> editorClass, EditorParams params) {
		final Editor editor;
		try {
			Constructor<? extends Editor> constructor = (Constructor<? extends Editor>) editorClass.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			editor = constructor.newInstance(params);
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
	public void setType(Class type) {
		this.type = type;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCategory(Object category) {
		this.category = category;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	private String getContextPath() {
		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		return request.getContextPath();
	}
}
