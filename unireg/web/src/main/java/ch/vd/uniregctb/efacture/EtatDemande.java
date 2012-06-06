package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;

public class EtatDemande extends AbstractEtat {

	private final String descriptionEtat;

	public EtatDemande(RegDate dateObtention, String motifObtention, String documentArchiveKey, String descriptionEtat) {
		super(dateObtention, motifObtention, documentArchiveKey);
		this.descriptionEtat = descriptionEtat;
	}

	public String getDescriptionEtat() {
		return descriptionEtat;
	}
}
