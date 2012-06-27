package ch.vd.uniregctb.efacture;

public enum TypeEtatDemande {
	RECUE,
	EN_ATTENTE_CONTACT,
	EN_ATTENTE_SIGNATURE,
	REFUSEE,
	IGNOREE,
	VALIDEE;

	public boolean isEnCours() {
		return this == EN_ATTENTE_CONTACT || this == EN_ATTENTE_SIGNATURE;
	}

	public boolean isValidable() {
		return this == EN_ATTENTE_SIGNATURE;
	}

	public boolean isRefusable() {
		return this == RECUE || this == EN_ATTENTE_CONTACT || this == EN_ATTENTE_SIGNATURE;
	}

	public boolean isMettableEnAttenteContact() {
		return this == RECUE || this == EN_ATTENTE_SIGNATURE;
	}

	public boolean isMettableEnAttenteSignature() {
		return this == RECUE || this == EN_ATTENTE_CONTACT;
	}
}
