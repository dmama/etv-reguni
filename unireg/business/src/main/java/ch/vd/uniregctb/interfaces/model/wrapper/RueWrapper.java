package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.Rue;

public class RueWrapper implements Rue {

	private final ch.vd.infrastructure.model.Rue target;

	public static RueWrapper get(ch.vd.infrastructure.model.Rue target) {
		if (target == null) {
			return null;
		}
		return new RueWrapper(target);
	}

	private RueWrapper(ch.vd.infrastructure.model.Rue target) {
		this.target = target;
	}

	public String getDesignationCourrier() {
		return target.getDesignationCourrier();
	}

	public Integer getNoLocalite() {
		return target.getNoLocalite();
	}

	public Integer getNoRue() {
		return target.getNoRue();
	}
}
