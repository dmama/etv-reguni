package ch.vd.uniregctb.listes.listesnominatives;

/**
 * Type d'adresse à fournir dans le batch des listes nominatives
 */
public enum TypeAdresse {

	/**
	 * Aucune adresse founie, juste le nom
	 */
	AUCUNE("Aucune adresse"),

	/**
	 * Les 6 lignes d'adresses sont fournie, déjà formattées
	 */
	FORMATTEE("Adresses formattées sur 6 lignes"),

	/**
	 * Les champs distincts des adresses sont fournis,
	 * le formattage éventuel sera assuré par le consommateur du fichier (Registre foncier)
	 */
	STRUCTUREE_RF("Adresses structurées pour le registre foncier");

	private final String description;

	private TypeAdresse(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
