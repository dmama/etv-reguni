package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "LOCALITE_POSTALE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmLocalitePostale extends RegpmEntity {

	private Long noOrdreP;
	private Integer npa;
	private Integer npaChiffreComplementaire;
	private String nomLong;
	private String nomCourt;
	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;

	@Id
	@Column(name = "NO_ORDRE_P")
	public Long getNoOrdreP() {
		return noOrdreP;
	}

	public void setNoOrdreP(Long noOrdreP) {
		this.noOrdreP = noOrdreP;
	}

	@Column(name = "NO_POSTAL_ACHEMIN")
	public Integer getNpa() {
		return npa;
	}

	public void setNpa(Integer npa) {
		this.npa = npa;
	}

	@Column(name = "CHIFFRE_COMPLEMENT")
	public Integer getNpaChiffreComplementaire() {
		return npaChiffreComplementaire;
	}

	public void setNpaChiffreComplementaire(Integer npaChiffreComplementaire) {
		this.npaChiffreComplementaire = npaChiffreComplementaire;
	}

	@Column(name = "NOM_COMPLET_MIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "39"))
	public String getNomLong() {
		return nomLong;
	}

	public void setNomLong(String nomLong) {
		this.nomLong = nomLong;
	}

	@Column(name = "DESIGN_ABREGEE_MIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "18"))
	public String getNomCourt() {
		return nomCourt;
	}

	public void setNomCourt(String nomCourt) {
		this.nomCourt = nomCourt;
	}

	@Column(name = "DAD_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Column(name = "DAF_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}
}
