package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.District;
import ch.vd.uniregctb.interfaces.model.Region;

public class DistrictImpl extends EntiteFiscaleImpl implements District {
	private Region region;

	public static DistrictImpl get(ch.vd.fidor.ws.v2.District target) {
		if (target == null) {
			return null;
		}
		return new DistrictImpl(target);
	}

	private DistrictImpl(ch.vd.fidor.ws.v2.District target) {
		super(target.getCode(), target.getDesignation());
		this.region = RegionImpl.get(target.getRegion());
	}

	@Override
	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}
}
