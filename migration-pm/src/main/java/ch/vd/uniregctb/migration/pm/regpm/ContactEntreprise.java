package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;

@Embeddable
@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class)
public class ContactEntreprise {

	private String nom;
	private String prenom;
	private String noTelephone;
	private String noFax;

	@Column(name = "NOM_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOM_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Column(name = "NO_TEL_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoTelephone() {
		return noTelephone;
	}

	public void setNoTelephone(String noTelephone) {
		this.noTelephone = noTelephone;
	}

	@Column(name = "NO_FAX_CONTACT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoFax() {
		return noFax;
	}

	public void setNoFax(String noFax) {
		this.noFax = noFax;
	}
}
