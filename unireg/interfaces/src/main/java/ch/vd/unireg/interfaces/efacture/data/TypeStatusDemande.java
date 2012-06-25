package ch.vd.unireg.interfaces.efacture.data;

public enum TypeStatusDemande {
	IGNOREE("Ignorée"),
	A_TRAITE("A traiter"),
	REFUSEE("Refusée"),
	VALIDATION_EN_COURS("Validation en cours"),
	VALIDEE("Validée");

	private String description;

	private TypeStatusDemande(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
