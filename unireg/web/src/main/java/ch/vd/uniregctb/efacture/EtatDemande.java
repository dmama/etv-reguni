package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDemande;

public class EtatDemande extends AbstractEtat {

	private final String descriptionEtat;
	private final TypeEtatDemande type;
	private TypeAttenteEFacture typeAttenteEFacture;


	public EtatDemande(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat, TypeEtatDemande type, TypeAttenteEFacture typeAttenteEFacture) {
		super(dateObtention, motifObtention, documentArchiveKey);
		this.descriptionEtat = descriptionEtat;
		this.type = type;
		this.typeAttenteEFacture = typeAttenteEFacture;
	}

	public String getDescriptionEtat() {
		return descriptionEtat;
	}

	public TypeEtatDemande getType() {
		return type;
	}

	public boolean isEnCours() {
		return this.type == TypeEtatDemande.VALIDATION_EN_COURS;
	}

	public boolean isValidable() {
		return this.type == TypeEtatDemande.VALIDATION_EN_COURS && this.typeAttenteEFacture == TypeAttenteEFacture.EN_ATTENTE_SIGNATURE;
	}

	public boolean isRefusable() {
		final  boolean EN_ATTENTE_CONTACT =this.type == TypeEtatDemande.VALIDATION_EN_COURS && this.typeAttenteEFacture == TypeAttenteEFacture.EN_ATTENTE_CONTACT ;
		final  boolean EN_ATTENTE_SIGNATURE = this.type == TypeEtatDemande.VALIDATION_EN_COURS && this.typeAttenteEFacture == TypeAttenteEFacture.EN_ATTENTE_SIGNATURE;
		return this.type == TypeEtatDemande.A_TRAITER || EN_ATTENTE_CONTACT ||EN_ATTENTE_SIGNATURE;
	}

	public boolean isMettableEnAttenteContact() {
		final  boolean EN_ATTENTE_SIGNATURE = this.type == TypeEtatDemande.VALIDATION_EN_COURS && this.typeAttenteEFacture == TypeAttenteEFacture.EN_ATTENTE_SIGNATURE;
		return this.type == TypeEtatDemande.A_TRAITER || EN_ATTENTE_SIGNATURE;
	}

	public boolean isMettableEnAttenteSignature() {
		final  boolean EN_ATTENTE_CONTACT =this.type == TypeEtatDemande.VALIDATION_EN_COURS && this.typeAttenteEFacture == TypeAttenteEFacture.EN_ATTENTE_CONTACT ;
		return this.type == TypeEtatDemande.A_TRAITER || EN_ATTENTE_CONTACT;
	}
}
