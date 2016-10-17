package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue("PersonnePhysique")
public class PersonnePhysiqueRF extends TiersRF {

	private String nom;
	private String prenom;
	private RegDate dateNaissance;

	@Column(name = "NOM_PP")
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOM_PP")
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Column(name = "DATE_NAISSANCE")
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}
}
