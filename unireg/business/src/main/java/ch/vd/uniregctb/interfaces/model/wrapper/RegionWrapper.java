package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.Region;

public class RegionWrapper implements Region {

	private final ch.vd.infrastructure.model.Region target;

	public static RegionWrapper get(ch.vd.infrastructure.model.Region target) {
		if (target == null) {
			return null;
		}
		return new RegionWrapper(target);
	}

	private RegionWrapper(ch.vd.infrastructure.model.Region target) {
		this.target = target;
	}

	public int getIdDirectionRegionale() {
		return target.getIdDirectionRegionale();
	}

	public int getIdTechnique() {
		return target.getIdTechnique();
	}

	public String getNomRegion() {
		return target.getNomRegion();
	}

	public String getSigle() {
		return target.getSigle();
	}

}
