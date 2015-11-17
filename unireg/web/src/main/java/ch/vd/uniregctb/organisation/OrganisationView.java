package ch.vd.uniregctb.organisation;

import java.io.Serializable;

/**
 *
 */
public class OrganisationView implements Serializable {

	private static final long serialVersionUID = 2125635768219037077L;

	private Long numeroOrganisation;
	private String numeroIDE;
	private String nom;
	private String formeJuridique;
	private Integer autoriteFiscale;

	private boolean canceled;
	private Long numeroOrganisationRemplacant;

	public Long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	public void setNumeroOrganisation(Long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getNumeroOrganisationRemplacant() {
		return numeroOrganisationRemplacant;
	}
	public void setNumeroOrganisationRemplacant(Long numeroOrganisationRemplacant) {
		this.numeroOrganisationRemplacant = numeroOrganisationRemplacant;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public String getNumeroIDE() {
		return numeroIDE;
	}

	public void setNumeroIDE(String numeroIDE) {
		this.numeroIDE = numeroIDE;
	}

	public String getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(String formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	public Integer getAutoriteFiscale() {
		return autoriteFiscale;
	}

	public void setAutoriteFiscale(Integer autoriteFiscale) {
		this.autoriteFiscale = autoriteFiscale;
	}

	/**
	 * Redefinition de la methode equals pour gerer les ajout/suppression d'objets dans une collection
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		// object must be Test at this point
		OrganisationView organisationView = (OrganisationView) obj;
		return (numeroOrganisation.equals(organisationView.numeroOrganisation) || numeroOrganisation.equals(organisationView.numeroOrganisation));
	}

	/**
	 * Redefinition de la methode hashCode pour gerer les ajout/suppression d'objets
	 * dans une collection
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash ;
		hash = 31 * hash + (null == numeroOrganisation ? 0 : numeroOrganisation.hashCode());
		return hash;
	}
}
