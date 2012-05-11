package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class RegionImpl extends EntiteFiscaleImpl implements Region, Serializable {

	private static final long serialVersionUID = 5665657861935801686L;

	public static RegionImpl get(ch.vd.fidor.ws.v2.Region target) {
		if (target == null) {
			return null;
		}
		return new RegionImpl(target);
	}

	@SuppressWarnings("UnusedDeclaration")
	private RegionImpl() {
		// pour la serialization
	}

	private RegionImpl(ch.vd.fidor.ws.v2.Region target) {
		super(target.getCode(), target.getDesignation());
	}
}
