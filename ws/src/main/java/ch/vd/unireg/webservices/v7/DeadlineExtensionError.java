package ch.vd.unireg.webservices.v7;

/**
 * Codes d'erreur d'une demande de délais groupées, tels que définis dans la spécification "SCU-ControlerDemandeDélai.doc".
 */
public enum DeadlineExtensionError {
	AUCUNE_DECLARATION("01", "Il n'y a pas de DI sur la période fiscale {PF}"),
	DECLARATION_ANNULEE("02", "La DI est annulée sur la période fiscale {PF}"),
	DECLARATION_SUSPENDUE("03", "La DI est suspendue sur la période fiscale {PF}"),
	DECLARATION_RETOURNEE("04", "La DI est déjà retournée sur la période fiscale {PF}"),
	PLUSIEURS_DECLARATIONS("05", "Il y a plusieurs DI en cours sur la période fiscale {PF}"),
	DECLARATION_SOMMEE("06", "La DI est déjà sommée sur la période fiscale {PF}"),
	DECLARATION_ECHUE("07", "La DI est déjà échue sur la période fiscale {PF}"),
	DECLARATION_RAPPELEE("08", "La DI est déjà rappelée sur la période fiscale {PF}"),
	DELAI_DEJA_ACCORDE("09", "Délai déjà accordé pour au {date délai} sur la période fiscale {PF}"),
	EN_ATTENTE("99", "Réponse en attente");

	private final String code;
	private final String defaultMessage;

	DeadlineExtensionError(String code, String defaultMessage) {
		this.code = code;
		this.defaultMessage = defaultMessage;
	}

	public String getCode() {
		return code;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}
}
