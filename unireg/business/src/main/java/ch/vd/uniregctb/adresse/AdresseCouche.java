package ch.vd.uniregctb.adresse;

public enum AdresseCouche {
	CIVILE("Civil"),
	PRINCIPAL("Principal Ménage"),
	CONTRIBUABLE("Contribuable associé"),
	DEFAUTS_CIVILES("Défauts adresses civiles"),
	REPRESENTANT("Représentation conventionnelle"),
	CONSEIL_LEGAL("Conseil légal"),
	TUTEUR("Tutelle"),
	CURATELLE("Curatelle"),
	REPRESENTANT_EXEC_FORCEE("Représentation avec exécution forcée"),
	FISCALE("Fiscal"),
	DEFAUTS_FISCALES("Défauts adresses fiscales"),
	RESULTAT("Résultat final");

	private final String description;

	AdresseCouche(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
