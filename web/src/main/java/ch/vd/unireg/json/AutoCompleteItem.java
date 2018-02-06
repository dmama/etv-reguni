package ch.vd.unireg.json;

/**
 * Structure de données attendue par le code Javascript Autocompete.generic (voir le fichier unireg.js)
 */
public class AutoCompleteItem {
	/**
	 * Chaîne de caractères utilisée dans le champ d'autocompletion
	 */
	private final String label;
	/**
	 * Chaîne de caractères utilisée dans la liste (dropdown) des valeurs disponibles
	 */
	private final String desc;
	/**
	 * Identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
	 */
	private String id1;
	/**
	 * Second identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
	 */
	private String id2;

	public AutoCompleteItem(String label, String desc) {
		this.label = label;
		this.desc = desc;
	}

	public AutoCompleteItem(String label, String desc, String id1) {
		this.label = label;
		this.desc = desc;
		this.id1 = id1;
	}

	public AutoCompleteItem(String label, String desc, String id1, String id2) {
		this.label = label;
		this.desc = desc;
		this.id1 = id1;
		this.id2 = id2;
	}

	public String getLabel() {
		return label;
	}

	public String getDesc() {
		return desc;
	}

	public String getId1() {
		return id1;
	}

	public String getId2() {
		return id2;
	}
}
