package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Rue;

public class RueWrapper implements Rue, Serializable {

	private static final long serialVersionUID = 7361240668934832807L;
	
	private String designationCourrier;
	private Integer noLocalite;
	private Integer noRue;

	public static RueWrapper get(ch.vd.infrastructure.model.Rue target) {
		if (target == null) {
			return null;
		}
		return new RueWrapper(target);
	}

	private RueWrapper(ch.vd.infrastructure.model.Rue target) {
		this.designationCourrier = target.getDesignationCourrier();
		this.noLocalite = target.getNoLocalite();
		this.noRue = target.getNoRue();
	}

	public String getDesignationCourrier() {
		return designationCourrier;
	}

	public Integer getNoLocalite() {
		return noLocalite;
	}

	public Integer getNoRue() {
		return noRue;
	}
}
