package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;

public class ObjectImpotUserType extends EnumCharMappingUserType<RegpmObjectImpot> {

	private static final Map<String, RegpmObjectImpot> MAPPING = buildMapping();

	private static Map<String, RegpmObjectImpot> buildMapping() {
		final Map<String, RegpmObjectImpot> map = new HashMap<>();
		map.put("IC", RegpmObjectImpot.CANTONAL);
		map.put("ICO", RegpmObjectImpot.COMMUNAL);
		map.put("IFD", RegpmObjectImpot.FEDERAL);
		return map;
	}

	public ObjectImpotUserType() {
		super(RegpmObjectImpot.class, MAPPING);
	}
}
