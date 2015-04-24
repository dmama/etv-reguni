package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;

public class TypeMandatUserType extends EnumCharMappingUserType<RegpmTypeMandat> {

	private static final Map<String, RegpmTypeMandat> MAPPING = buildMapping();

	private static Map<String, RegpmTypeMandat> buildMapping() {
		final Map<String, RegpmTypeMandat> map = new HashMap<>();
		map.put("G", RegpmTypeMandat.GENERAL);
		map.put("S", RegpmTypeMandat.SPECIAL);
		map.put("C", RegpmTypeMandat.COMPTABLE);
		map.put("T", RegpmTypeMandat.TIERS);
		return map;
	}

	public TypeMandatUserType() {
		super(RegpmTypeMandat.class, MAPPING);
	}
}
