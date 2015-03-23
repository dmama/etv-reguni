package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.fidor.xml.post.v1.Street;

public class RueImpl implements Rue, Serializable {

	private static final long serialVersionUID = 723446726077715687L;
	
	private final String designationCourrier;
	private final Integer noLocalite;
	private final Integer noRue;

	public static RueImpl get(ch.vd.infrastructure.model.Rue target) {
		if (target == null) {
			return null;
		}
		return new RueImpl(target);
	}

	public static RueImpl get(Street target) {
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

	private RueImpl(Street target) {
		this.designationCourrier = target.getLongName();
		this.noLocalite = target.getSwissZipCodeId().get(0);        // TODO on prend la première...
		this.noRue = target.getEstrid();
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
