package ch.vd.uniregctb.efacture;

public enum TypeRefusEFacture {

	NUMERO_CTB_INCOHERENT("Numéro de contribuable incohérent"),
	NUMERO_AVS_CTB_INCOHERENT("Numéro AVS incohérent avec le numéro de contribuable"),
	ADRESSE_COURRIER_INEXISTANTE("Aucune adresse courrier pour ce contribuable");
	String description;

	private TypeRefusEFacture(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
