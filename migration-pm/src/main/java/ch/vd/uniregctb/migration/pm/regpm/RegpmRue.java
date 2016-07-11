package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "RUE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmRue extends RegpmEntity implements WithLongId {

	private Long id;
	private String radical;
	private String designation;
	private String designationCourrier;
	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private RegpmLocalitePostale localitePostale;

	@Id
	@Column(name = "NO_RUE")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "RADICAL")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getRadical() {
		return radical;
	}

	public void setRadical(String radical) {
		this.radical = radical;
	}

	@Column(name = "DESIGN_MIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "28"))
	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	@Column(name = "DESIGN_COURRIER")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "25"))
	public String getDesignationCourrier() {
		return designationCourrier;
	}

	public void setDesignationCourrier(String designationCourrier) {
		this.designationCourrier = designationCourrier;
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

	@ManyToOne
	@JoinColumn(name = "FK_LOCPOSNO")
	public RegpmLocalitePostale getLocalitePostale() {
		return localitePostale;
	}

	public void setLocalitePostale(RegpmLocalitePostale localitePostale) {
		this.localitePostale = localitePostale;
	}
}
