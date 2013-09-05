package ch.vd.uniregctb.efacture;

import java.util.Date;

public class EtatDestinataireView extends AbstractEtatView {

	private final String descriptionEtat;
	private final String email;

	public EtatDestinataireView(Date dateObtention, String motifObtention, ArchiveKey documentArchiveKey, String descriptionEtat, String email) {
		super(dateObtention, motifObtention, documentArchiveKey);
		this.descriptionEtat = descriptionEtat;
		this.email = email;
	}

	public String getDescriptionEtat() {
		return descriptionEtat;
	}

	public String getEmail() {
		return email;
	}
}
