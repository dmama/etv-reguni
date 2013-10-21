package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class RueImpl implements Rue, Serializable {

	private static final long serialVersionUID = -19873225126550620L;
	
	private final String designationCourrier;
	private final Integer noLocalite;
	private final Integer noRue;

	public static RueImpl get(ch.vd.infrastructure.model.Rue target) {
		if (target == null) {
			return null;
		}
		return new RueImpl(target);
	}

	private RueImpl(ch.vd.infrastructure.model.Rue target) {
		this.designationCourrier = target.getDesignationCourrier();
		this.noLocalite = target.getNoLocalite();
		this.noRue = target.getNoRue();
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
	public String toString() {
		return String.format("RueImpl{designationCourrier='%s', noLocalite=%d, noRue=%d}", designationCourrier, noLocalite, noRue);
	}
}
