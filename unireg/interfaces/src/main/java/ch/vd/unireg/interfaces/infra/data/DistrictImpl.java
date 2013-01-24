package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

import ch.vd.evd0012.v1.DistrictFiscal;

public class DistrictImpl extends EntiteFiscaleImpl implements District, Serializable {

	private static final long serialVersionUID = -7321126594707702915L;

	private Integer codeRegion;

	public static DistrictImpl get(ch.vd.fidor.ws.v2.District target) {
		if (target == null) {
			return null;
		}
		return new DistrictImpl(target);
	}

	public static District get(DistrictFiscal target) {
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
		this.codeRegion = target.getRegion() == null ? null : target.getRegion().getCode();
	}

	public DistrictImpl(DistrictFiscal target) {
		super(target.getCode(), target.getDesignation());
		this.codeRegion = parseCodeRegion(target.getRegionFiscaleLink());
	}

	private static Integer parseCodeRegion(String link) {
		if (StringUtils.isBlank(link)) {
			return null;
		}
		// e.g. "regionFiscale/1" => 1
		return Integer.parseInt(link.substring(link.indexOf('/') + 1));
	}

	@Override
	public Integer getCodeRegion() {
		return codeRegion;
	}
}
