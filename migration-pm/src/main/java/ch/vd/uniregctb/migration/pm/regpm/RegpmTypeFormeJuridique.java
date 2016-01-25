package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table(name = "FORME_JURIDIQ_ACI")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmTypeFormeJuridique extends RegpmEntity {

	private String numero;
	private String code;
	private String designation;
	private RegpmCategoriePersonneMorale categorie;
	private RegDate dateAnnulation;

	@Column(name = "NO_FORME_JURID_ACI")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "2"))
	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	@Id
	@Column(name = "CO_FORME_JURID")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "DESIGNATION")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "25"))
	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	@Column(name = "CO_CATEG_PERSONNE", columnDefinition = "char", length = 3)
	@Enumerated(value = EnumType.STRING)
	public RegpmCategoriePersonneMorale getCategorie() {
		return categorie;
	}

	public void setCategorie(RegpmCategoriePersonneMorale categorie) {
		this.categorie = categorie;
	}

	@Column(name = "DATE_ANNULATION")
	@Type(type = "RegDate")
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}
}
