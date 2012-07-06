package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.RegistrationRequestStatus;

/**
 * Type relatif à la e-facture
 */
public enum TypeEtatDemande {
	A_TRAITER (TypeAttenteDemande.PAS_EN_ATTENTE),
	VALIDATION_EN_COURS (TypeAttenteDemande.PAS_EN_ATTENTE),
	VALIDATION_EN_COURS_EN_ATTENTE_CONTACT(TypeAttenteDemande.EN_ATTENTE_CONTACT), // état interne unireg qui n'a pas d'équivalent e-facture
	VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE(TypeAttenteDemande.EN_ATTENTE_SIGNATURE), // état interne unireg qui n'a pas d'équivalent e-facture
	REFUSEE (TypeAttenteDemande.PAS_EN_ATTENTE),
	IGNOREE (TypeAttenteDemande.PAS_EN_ATTENTE),
	VALIDEE (TypeAttenteDemande.PAS_EN_ATTENTE);


	private TypeAttenteDemande typeAttente;

	TypeEtatDemande(TypeAttenteDemande type) {
		this.typeAttente = type;
	}

	public TypeAttenteDemande getTypeAttente() {
		return typeAttente;
	}

	public boolean isEnAttente() {
		return  this == VALIDATION_EN_COURS_EN_ATTENTE_CONTACT ||
				this == VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE;
	}

	public boolean isEnCours() {
		return  this == VALIDATION_EN_COURS_EN_ATTENTE_CONTACT ||
				this == VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE ||
				this == VALIDATION_EN_COURS;
	}

	public boolean isValidable() {
		return this == VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE;
	}

	public boolean isRefusable() {
		return  this == A_TRAITER ||
				this == VALIDATION_EN_COURS_EN_ATTENTE_CONTACT ||
				this == VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE ||
				this == VALIDATION_EN_COURS;
	}

	public boolean isMettableEnAttenteContact() {
		return  this == A_TRAITER ||
				this == VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE ||
				this == VALIDATION_EN_COURS;
	}

	public boolean isMettableEnAttenteSignature() {
		return  this == A_TRAITER ||
				this == VALIDATION_EN_COURS_EN_ATTENTE_CONTACT ||
				this == VALIDATION_EN_COURS;
	}

	public static TypeEtatDemande valueOf (RegistrationRequestStatus status, TypeAttenteDemande typeAttente ) {
		switch (status){
		case IGNOREE:
			return TypeEtatDemande.IGNOREE;
		case A_TRAITER:
			return TypeEtatDemande.A_TRAITER;
		case REFUSEE:
			return TypeEtatDemande.REFUSEE;
		case VALIDATION_EN_COURS:
			if (typeAttente == TypeAttenteDemande.PAS_EN_ATTENTE) {
				return TypeEtatDemande.VALIDATION_EN_COURS;
			} else if (typeAttente == TypeAttenteDemande.EN_ATTENTE_SIGNATURE) {
				return VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE;
			} else if (typeAttente == TypeAttenteDemande.EN_ATTENTE_CONTACT) {
				return VALIDATION_EN_COURS_EN_ATTENTE_CONTACT;
			}
			throw new AssertionError("Impossible d'atterrir ici, tout les valeurs possibles de l'enum TypeAttenteDemande sont traités en amont !");
		case VALIDEE:
			return TypeEtatDemande.VALIDEE;
		default:
			throw new IllegalArgumentException("Le statut de demande suivant n'est pas reconnu "+ status);
		}
	}
}
