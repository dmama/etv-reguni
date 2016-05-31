package ch.vd.uniregctb.type;

/**
 * Les types des différents états possibles d'une entreprise
 */
public enum TypeEtatEntreprise {
	INSCRITE_RC("Inscrite au RC"),
	EN_LIQUIDATION("En liquidation"),
	EN_FAILLITE("En faillite"),
	ABSORBEE("Absorbée"),
	RADIEE_RC("Radiée du RC"),
	FONDEE("Fondée"),
	DISSOUTE("Dissoute");

	private final String libelle;

	TypeEtatEntreprise(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}
