package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class Partie {

	@NotNull
	private final Personne personne;
	private final Adresse adresse;
	private final LieuDeResidence lieuDeResidence;

	public Partie(@NotNull Personne personne, Adresse adresse, LieuDeResidence lieuDeResidence) {
		this.personne = personne;
		this.adresse = adresse;
		this.lieuDeResidence = lieuDeResidence;
	}

	public Adresse getAdresse() {
		return adresse;
	}

	public LieuDeResidence getLieuDeResidence() {
		return lieuDeResidence;
	}

	@NotNull
	public Personne getPersonne() {
		return personne;
	}
}
