package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

public class FonctionOrganisation {

	/*
		Le numéro cantonal de l'individu (RCPers)
	 */
	private final Integer numeroIndividu;
	@NotNull
	private final String nom;
	private final String prenom;

	private final String lieuDeResidence;

	private final String textFonction;
	private final Autorisation autorisation;
	private final String restrictionAutorisation;

	public FonctionOrganisation(Integer numeroIndividu, @NotNull String nom, String prenom, String lieuDeResidence, String textFonction, Autorisation autorisation, String restrictionAutorisation) {
		this.numeroIndividu = numeroIndividu;
		this.nom = nom;
		this.prenom = prenom;
		this.lieuDeResidence = lieuDeResidence;
		this.textFonction = textFonction;
		this.autorisation = autorisation;
		this.restrictionAutorisation = restrictionAutorisation;
	}

	public Integer getNumeroIndividu() {
		return numeroIndividu;
	}

	@NotNull
	public String getNom() {
		return nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public String getLieuDeResidence() {
		return lieuDeResidence;
	}

	public String getTextFonction() {
		return textFonction;
	}

	public Autorisation getAutorisation() {
		return autorisation;
	}

	public String getRestrictionAutorisation() {
		return restrictionAutorisation;
	}
}
