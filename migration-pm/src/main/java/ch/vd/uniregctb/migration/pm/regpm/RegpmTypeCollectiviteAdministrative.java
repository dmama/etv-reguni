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
@Table(name = "TY_COLL_ADM")
@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class)
public class RegpmTypeCollectiviteAdministrative extends RegpmEntity implements WithLongId {

	private Long id;
	private String sigle;
	private String designation;

	@Id
	@Column(name = "NO_TECHNIQUE")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "SIGLE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "5"))
	public String getSigle() {
		return sigle;
	}

	public void setSigle(String sigle) {
		this.sigle = sigle;
	}

	@Column(name = "DESIGNATION")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}
}
