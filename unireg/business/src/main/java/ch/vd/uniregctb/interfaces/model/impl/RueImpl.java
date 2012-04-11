package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Rue;

public class RueImpl implements Rue, Serializable {

	private static final long serialVersionUID = -5307681617982000774L;
	
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
}
