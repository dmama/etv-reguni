package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;

public class ModeImpositionUserType extends EnumCharMappingUserType<RegpmModeImposition> {

	private static final Map<String, RegpmModeImposition> MAPPING = buildMapping();

	private static Map<String, RegpmModeImposition> buildMapping() {
		final Map<String, RegpmModeImposition> map = new HashMap<>();
		map.put("PRAE", RegpmModeImposition.PRAE);
		map.put("POST", RegpmModeImposition.POST);
		return map;
	}

	public ModeImpositionUserType() {
		super(RegpmModeImposition.class, MAPPING);
	}

}
