package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;

@Entity
@Table(name = "TY_REGIME_FISCAL")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class)
})
public class RegpmTypeRegimeFiscal extends RegpmEntity {

	private String code;
	private String libelleLong;
	private String libelleCourt;

	@Id
	@Column(name = "CODE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "5"))
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "LIBELLE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getLibelleLong() {
		return libelleLong;
	}

	public void setLibelleLong(String libelleLong) {
		this.libelleLong = libelleLong;
	}

	@Column(name = "LIB_ABREGE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getLibelleCourt() {
		return libelleCourt;
	}

	public void setLibelleCourt(String libelleCourt) {
		this.libelleCourt = libelleCourt;
	}
}
