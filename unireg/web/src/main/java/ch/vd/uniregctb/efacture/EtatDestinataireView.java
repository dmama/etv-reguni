package ch.vd.uniregctb.efacture;

import java.util.Date;

public class EtatDestinataireView extends AbstractEtatView {

	private final String descriptionEtat;

	public EtatDestinataireView(Date dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat) {
		super(dateObtention, motifObtention, documentArchiveKey);
		this.descriptionEtat = descriptionEtat;
	}

	public String getDescriptionEtat() {
		return descriptionEtat;
	}
}
