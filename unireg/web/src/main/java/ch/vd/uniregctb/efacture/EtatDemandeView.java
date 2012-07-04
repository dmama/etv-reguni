package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;

public class EtatDemandeView extends AbstractEtatView {

	private final String descriptionEtat;
	private final TypeEtatDemande type;

	public EtatDemandeView(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat, TypeEtatDemande type) {
		super(dateObtention, motifObtention, documentArchiveKey);
		this.descriptionEtat = descriptionEtat;
		this.type = type;
	}

	public String getDescriptionEtat() {
		return descriptionEtat;
	}

	public TypeEtatDemande getType() {
		return type;
	}

	public boolean isEnCours() {
		return type.isEnCours();
	}

	public boolean isValidable() {
		return type.isValidable();
	}

	public boolean isRefusable() {
		return type.isRefusable();
	}

	public boolean isMettableEnAttenteContact() {
		return type.isMettableEnAttenteContact();
	}

	public boolean isMettableEnAttenteSignature() {
		return type.isMettableEnAttenteSignature();
	}
}
