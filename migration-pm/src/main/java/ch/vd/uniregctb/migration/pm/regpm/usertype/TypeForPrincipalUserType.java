package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;

public class TypeForPrincipalUserType extends EnumCharMappingUserType<RegpmTypeForPrincipal> {

	private static final Map<String, RegpmTypeForPrincipal> MAPPING = buildMapping();

	private static Map<String, RegpmTypeForPrincipal> buildMapping() {
		final Map<String, RegpmTypeForPrincipal> map = new HashMap<>();
		map.put("A", RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE);
		map.put("S", RegpmTypeForPrincipal.SIEGE);
		return map;
	}

	public TypeForPrincipalUserType() {
		super(RegpmTypeForPrincipal.class, MAPPING);
	}
}
