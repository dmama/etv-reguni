package ch.vd.uniregctb.efacture;

public enum TypeRefusEFacture {

	NUMERO_AVS_INVALIDE("Numéro AVS invalide"),
	EMAIL_INVALIDE("Adresse de courrier éléctronique invalide"),
	DATE_DEMANDE_ABSENTE("Date de la demande non renseignée"),
	NUMERO_CTB_INCOHERENT("Numéro de contribuable incohérent"),
	NUMERO_AVS_CTB_INCOHERENT("Numéro AVS incohérent avec le numéro de contribuable"),
	ADRESSE_COURRIER_INEXISTANTE("Aucune adresse courrier pour ce contribuable");
	private String description;

	private TypeRefusEFacture(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
