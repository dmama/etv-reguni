package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.evd0012.v1.RegionFiscale;

public class RegionImpl extends EntiteFiscaleImpl implements Region, Serializable {

	private static final long serialVersionUID = 384550718999168176L;

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

	public RegionImpl(RegionFiscale target) {
		super(target.getCode(), target.getDesignation());
	}
}
