package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;

public class EtatDemandeView extends AbstractEtat {

	private final String descriptionEtat;
	private final TypeEtatDemande type;
	private TypeAttenteDemande typeAttenteEFacture;


	public EtatDemandeView(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat, TypeEtatDemande type, TypeAttenteDemande typeAttenteEFacture) {
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
		return this.type == TypeEtatDemande.VALIDATION_EN_COURS && this.typeAttenteEFacture == TypeAttenteDemande.EN_ATTENTE_SIGNATURE;
	}

	public boolean isRefusable() {
		return this.type == TypeEtatDemande.VALIDATION_EN_COURS;
	}

	public boolean isMettableEnAttenteContact() {
		final boolean EN_ATTENTE_SIGNATURE = (this.type == TypeEtatDemande.VALIDATION_EN_COURS) && (this.typeAttenteEFacture == TypeAttenteDemande.EN_ATTENTE_SIGNATURE);
		return this.type == TypeEtatDemande.VALIDATION_EN_COURS || EN_ATTENTE_SIGNATURE;
	}

	public boolean isMettableEnAttenteSignature() {
		final boolean EN_ATTENTE_CONTACT = (this.type == TypeEtatDemande.VALIDATION_EN_COURS) && (this.typeAttenteEFacture == TypeAttenteDemande.EN_ATTENTE_CONTACT);
		return this.type == TypeEtatDemande.VALIDATION_EN_COURS || EN_ATTENTE_CONTACT;
	}
}
