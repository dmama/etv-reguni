package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;

public class EtatDestinataire extends AbstractEtat {

	private final String descriptionEtat;

	public EtatDestinataire(RegDate dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat) {
		super(dateObtention, motifObtention, documentArchiveKey);
		this.descriptionEtat = descriptionEtat;
	}

	public String getDescriptionEtat() {
		return descriptionEtat;
	}
}
