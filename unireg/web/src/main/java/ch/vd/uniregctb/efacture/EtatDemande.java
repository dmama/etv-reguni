package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;

public class EtatDemande extends AbstractEtat {

	private final String descriptionEtat;
	private final TypeEtatDemande type;

	public EtatDemande(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat, TypeEtatDemande type) {
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
}
