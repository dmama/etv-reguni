package ch.vd.unireg.efacture;

import java.util.Date;

public class EtatDestinataireView extends AbstractEtatView {

	private final String descriptionEtat;
	private final String email;

	public EtatDestinataireView(Date dateObtention, String motifObtention, String descriptionEtat, String email) {
		super(dateObtention, motifObtention, null, null);
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
