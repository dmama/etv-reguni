package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "RADIATION_RC_VD")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
})
public class RadiationRC extends RegpmEntity implements Comparable<RadiationRC>, WithLongId {

	private Long id;
	private RegDate dateRadiation;
	private boolean rectifiee;

	@Override
	public int compareTo(@NotNull RadiationRC o) {
		return NullDateBehavior.EARLIEST.compare(dateRadiation, o.dateRadiation);
	}

	@Id
	@Column(name = "NO_RAD_RC_VD")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DA_RAD_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateRadiation() {
		return dateRadiation;
	}

	public void setDateRadiation(RegDate dateRadiation) {
		this.dateRadiation = dateRadiation;
	}

	@Column(name = "RECTIFIEE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifiee() {
		return rectifiee;
	}

	public void setRectifiee(boolean rectifiee) {
		this.rectifiee = rectifiee;
	}
}
