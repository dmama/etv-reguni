package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.evd0012.v1.RegionFiscale;

public class RegionImpl extends EntiteFiscaleImpl implements Region, Serializable {

	private static final long serialVersionUID = 2169126061399455534L;

	public static RegionImpl get(ch.vd.fidor.ws.v2.Region target) {
		if (target == null) {
			return null;
		}
		return new RegionImpl(target);
	}

	public static Region get(RegionFiscale target) {
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

	public RegionImpl(RegionFiscale target) {
		super(target.getCode(), target.getDesignation());
	}
}
