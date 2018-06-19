package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

/**
 * La fonction d'une personne physique dans l'entreprise d'une entreprise.
 */
public class FonctionOrganisation implements Serializable {

	private static final long serialVersionUID = 4996250856949474474L;

	/**
	 * Le numéro cantonal de l'individu (RCPers)
	 */
	private final Integer idCantonalIndividu;
	@NotNull
	private final String nom;
	private final String prenom;

	private final String lieuDeResidence;

	private final String textFonction;
	private final Autorisation autorisation;
	private final String restrictionAutorisation;

	public FonctionOrganisation(Integer idCantonalIndividu, @NotNull String nom, String prenom, String lieuDeResidence, String textFonction, Autorisation autorisation, String restrictionAutorisation) {
		this.idCantonalIndividu = idCantonalIndividu;
		this.nom = nom;
		this.prenom = prenom;
		this.lieuDeResidence = lieuDeResidence;
		this.textFonction = textFonction;
		this.autorisation = autorisation;
		this.restrictionAutorisation = restrictionAutorisation;
	}

	public Integer getIdCantonalIndividu() {
		return idCantonalIndividu;
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
