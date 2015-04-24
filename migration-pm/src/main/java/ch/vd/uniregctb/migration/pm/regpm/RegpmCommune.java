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

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.IntegerAsFixedCharUserType;

@Entity
@Table(name = "COMMUNE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "IntegerAsFixedChar", typeClass = IntegerAsFixedCharUserType.class)
})
public class RegpmCommune extends RegpmEntity implements WithLongId {

	private Long id;
	private Integer noOfs;
	private String nom;
	private RegpmCanton canton;

	@Id
	@Column(name = "NO_TECHNIQUE")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NO_OFS")
	@Type(type = "IntegerAsFixedChar")
	public Integer getNoOfs() {
		return noOfs;
	}

	public void setNoOfs(Integer noOfs) {
		this.noOfs = noOfs;
	}

	@Column(name = "NOM_OFS_MIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "25"))
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "FK_CANTONSIGLE")
	@Enumerated(value = EnumType.STRING)
	public RegpmCanton getCanton() {
		return canton;
	}

	public void setCanton(RegpmCanton canton) {
		this.canton = canton;
	}
}
