package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue("PersonnePhysique")
public class PersonnePhysiqueRF extends TiersRF {

	private String nom;
	private String prenom;
	private RegDate dateNaissance;

	@Column(name = "NOM_PP", length = LengthConstants.RF_NOM_PP)
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOM_PP", length = LengthConstants.RF_PRENOM_PP)
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Column(name = "DATE_NAISSANCE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType", parameters = {@Parameter(name = "allowPartial", value = "true")})
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final PersonnePhysiqueRF ppRight = (PersonnePhysiqueRF) right;
		ppRight.nom = this.nom;
		ppRight.prenom = this.prenom;
		ppRight.dateNaissance = this.dateNaissance;
	}
}
