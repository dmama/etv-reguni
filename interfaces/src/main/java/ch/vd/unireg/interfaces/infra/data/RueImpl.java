package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.fidor.xml.common.v1.Range;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.fidor.XmlUtils;

public class RueImpl implements Rue, Serializable {

	private static final long serialVersionUID = 3800908460552624913L;
	
	private final String designationCourrier;
	private final Integer noLocalite;
	private final Integer noRue;
	private final DateRange validite;

	public static RueImpl get(Street target) {
		if (target == null) {
			return null;
		}
		return new RueImpl(target);
	}

	private RueImpl(Street target) {
		this.designationCourrier = target.getLongName();
		this.noLocalite = target.getSwissZipCodeId().get(0);
		this.noRue = target.getEstrid();

		final Range validity = target.getValidity();
		if (validity != null) {
			this.validite = new DateRangeHelper.Range(XmlUtils.toRegDate(validity.getDateFrom()), XmlUtils.toRegDate(validity.getDateTo()));
		}
		else {
			this.validite = null;
		}
	}

	@Override
	public String getDesignationCourrier() {
		return designationCourrier;
	}

	@Override
	public Integer getNoLocalite() {
		return noLocalite;
	}

	@Override
	public Integer getNoRue() {
		return noRue;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return validite == null || validite.isValidAt(date);
	}

	@Override
	public RegDate getDateDebut() {
		return validite == null ? null : validite.getDateDebut();
	}

	@Override
	public RegDate getDateFin() {
		return validite == null ? null : validite.getDateFin();
	}

	@Override
	public String toString() {
		return String.format("RueImpl{designationCourrier='%s', noLocalite=%d, noRue=%d}", designationCourrier, noLocalite, noRue);
	}
}
