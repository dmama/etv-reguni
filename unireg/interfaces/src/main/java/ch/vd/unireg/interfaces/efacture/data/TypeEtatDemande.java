package ch.vd.unireg.interfaces.efacture.data;

/**
 * Type relatif à la e-facture
 */
public enum TypeEtatDemande {
	A_TRAITER,
	EN_ATTENTE_CONTACT,
	EN_ATTENTE_SIGNATURE,
	REFUSEE,
	IGNOREE,
	VALIDEE,
	VALIDATION_EN_COURS;


	public boolean isEnCours() {
		return this == EN_ATTENTE_CONTACT || this == EN_ATTENTE_SIGNATURE || this == VALIDATION_EN_COURS;
	}

	public boolean isValidable() {
		return this == EN_ATTENTE_SIGNATURE;
	}

	public boolean isRefusable() {
		return this == A_TRAITER || this == EN_ATTENTE_CONTACT || this == EN_ATTENTE_SIGNATURE || this == VALIDATION_EN_COURS;
	}

	public boolean isMettableEnAttenteContact() {
		return this == A_TRAITER || this == EN_ATTENTE_SIGNATURE || this == VALIDATION_EN_COURS;
	}

	public boolean isMettableEnAttenteSignature() {
		return this == A_TRAITER || this == EN_ATTENTE_CONTACT || this == VALIDATION_EN_COURS;
	}
}
