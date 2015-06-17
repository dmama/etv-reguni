package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class Partie {

	private final Integer cantonalIdPersonne;

	private final Personne personne;
	private final Adresse adresse;
	private final LieuDeResidence lieuDeResidence;

	public Partie(Integer cantonalIdPersonne, Personne personne, Adresse adresse, LieuDeResidence lieuDeResidence) {
		this.cantonalIdPersonne = cantonalIdPersonne;
		this.personne = personne;
		this.adresse = adresse;
		this.lieuDeResidence = lieuDeResidence;
	}

	public LieuDeResidence getLieuDeResidence() {
		return lieuDeResidence;
	}

	@NotNull
	public Personne getPersonne() {
		return personne;
	}
}
