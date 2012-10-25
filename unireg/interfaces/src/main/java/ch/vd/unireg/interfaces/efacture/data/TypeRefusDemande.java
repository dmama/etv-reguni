package ch.vd.unireg.interfaces.efacture.data;

public enum TypeRefusDemande {

	NUMERO_AVS_INVALIDE("Numéro AVS invalide."),
	EMAIL_INVALIDE("Adresse de courrier électronique invalide."),
	AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT("Une autre demande est déjà en cours de traitement."),
	DATE_DEMANDE_ABSENTE("Date de la demande non renseignée."),
	NUMERO_CTB_INCOHERENT("Numéro de contribuable incohérent."),
	NUMERO_AVS_CTB_INCOHERENT("Numéro AVS incohérent avec le numéro de contribuable."),
	ADRESSE_COURRIER_INEXISTANTE("Aucune adresse courrier pour ce contribuable."), ;

	private final String description;

	private TypeRefusDemande(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
