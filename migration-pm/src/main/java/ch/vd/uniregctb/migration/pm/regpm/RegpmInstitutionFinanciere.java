package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;

@Entity
@Table(name = "INSTIT_FIN")
@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class)
public class RegpmInstitutionFinanciere extends RegpmEntity implements WithLongId {

	private Long id;
	private String noIdentificationDTA;
	private String noClearing;
	private String nom;
	private String adresse1;
	private String adresse2;
	private String adresse3;
	private String noCompteBeneficiaire;

	@Id
	@Column(name = "NO_INSTIT_FIN")
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NO_IDENTIF_DTA")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "5"))
	public String getNoIdentificationDTA() {
		return noIdentificationDTA;
	}

	public void setNoIdentificationDTA(String noIdentificationDTA) {
		this.noIdentificationDTA = noIdentificationDTA;
	}

	@Column(name = "NO_CLEARING")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "9"))
	public String getNoClearing() {
		return noClearing;
	}

	public void setNoClearing(String noClearing) {
		this.noClearing = noClearing;
	}

	@Column(name = "NOM_INSTIT_FIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "ADRESSE_1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getAdresse1() {
		return adresse1;
	}

	public void setAdresse1(String adresse1) {
		this.adresse1 = adresse1;
	}

	@Column(name = "ADRESSE_2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getAdresse2() {
		return adresse2;
	}

	public void setAdresse2(String adresse2) {
		this.adresse2 = adresse2;
	}

	@Column(name = "ADRESSE_3")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getAdresse3() {
		return adresse3;
	}

	public void setAdresse3(String adresse3) {
		this.adresse3 = adresse3;
	}

	@Column(name = "NO_COMPTE_BENEFICI")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNoCompteBeneficiaire() {
		return noCompteBeneficiaire;
	}

	public void setNoCompteBeneficiaire(String noCompteBeneficiaire) {
		this.noCompteBeneficiaire = noCompteBeneficiaire;
	}
}
