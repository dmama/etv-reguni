package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

/**
 * Classe composant de Fonction associant une personne à une adresse et
 * un identifiant cantonal.
 *
 * 3 cas possibles:
 * - La personne est résident vaudois. Dans ce cas, le cantonalId est renseigné et les autres
 *   champs représentent une répétition des données de RCPers.
 * - La personne est un ancien résident vaudois. Dans ce cas, le cantonalId est renseigné et
 *   les autres champs contiennent les données actuelles de la personne.
 * - La personne n'a jamais été résident vaudois. Dans ce cas, le cantonalId est vide et
 *   les autres champs contiennent les seules données qu'on connaisse de la personne.
 */
public class Partie {

	private final Integer cantonalIdPersonne;
	@NotNull
	private final Personne personne;

	private final Adresse adresse;
	private final LieuDeResidence lieuDeResidence;

	public Partie(Integer cantonalIdPersonne, @NotNull Personne personne, Adresse adresse, LieuDeResidence lieuDeResidence) {
		this.cantonalIdPersonne = cantonalIdPersonne;
		this.personne = personne;
		this.adresse = adresse;
		this.lieuDeResidence = lieuDeResidence;
	}

	public LieuDeResidence getLieuDeResidence() {
		return lieuDeResidence;
	}

	public Integer getCantonalIdPersonne() {
		return cantonalIdPersonne;
	}

	@NotNull
	public Personne getPersonne() {
		return personne;
	}

	public Adresse getAdresse() {
		return adresse;
	}

}
