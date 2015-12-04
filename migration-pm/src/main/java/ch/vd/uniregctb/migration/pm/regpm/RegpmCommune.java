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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RegpmCommune that = (RegpmCommune) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (noOfs != null ? !noOfs.equals(that.noOfs) : that.noOfs != null) return false;
		if (nom != null ? !nom.equals(that.nom) : that.nom != null) return false;
		return canton == that.canton;

	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (noOfs != null ? noOfs.hashCode() : 0);
		result = 31 * result + (nom != null ? nom.hashCode() : 0);
		result = 31 * result + (canton != null ? canton.hashCode() : 0);
		return result;
	}
}
