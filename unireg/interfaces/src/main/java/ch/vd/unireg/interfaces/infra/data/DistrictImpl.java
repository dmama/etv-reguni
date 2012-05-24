package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class DistrictImpl extends EntiteFiscaleImpl implements District, Serializable {

	private static final long serialVersionUID = -3164487570387673899L;

	private Region region;

	public static DistrictImpl get(ch.vd.fidor.ws.v2.District target) {
		if (target == null) {
			return null;
		}
		return new DistrictImpl(target);
	}

	@SuppressWarnings("UnusedDeclaration")
	private DistrictImpl() {
		// pour la serialization
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
