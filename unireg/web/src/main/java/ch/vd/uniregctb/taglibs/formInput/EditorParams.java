package ch.vd.uniregctb.taglibs.formInput;

class EditorParams {

	private String id;

	private String path;

	/**
	 * Le type de la valeur saisie
	 */
	private Class type;

	/**
	 * (Optionnel) catégorie de haut niveau (NPA, numéro de rue, type d'entité, ...) permettant d'instancier un éditeur spécifique.
	 */
	private Object categorie;

	private boolean readonly;

	private String contextPath;

	EditorParams(String id, String path, Class type, Object categorie, boolean readonly, String contextPath) {
		this.id = id;
		this.path = path;
		this.type = type;
		this.categorie = categorie;
		this.readonly = readonly;
		this.contextPath = contextPath;
	}

	public String getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public Class getType() {
		return type;
	}

	public Object getCategorie() {
		return categorie;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public String getContextPath() {
		return contextPath;
	}
}
