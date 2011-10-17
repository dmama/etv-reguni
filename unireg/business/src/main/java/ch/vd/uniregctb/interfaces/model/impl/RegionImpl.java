package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.Region;

public class RegionImpl extends EntiteFiscaleImpl implements Region {
	public static RegionImpl get(ch.vd.fidor.ws.v2.Region target) {
		if (target == null) {
			return null;
		}
		return new RegionImpl(target);
	}

	private RegionImpl(ch.vd.fidor.ws.v2.Region target) {
		super(target.getCode(), target.getDesignation());
	}
}
