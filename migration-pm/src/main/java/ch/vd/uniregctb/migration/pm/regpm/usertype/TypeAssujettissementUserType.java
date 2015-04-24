package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;

public class TypeAssujettissementUserType extends EnumIntegerMappingUserType<RegpmTypeAssujettissement> {

	private static final Map<Integer, RegpmTypeAssujettissement> MAPPING = buildMapping();

	private static Map<Integer, RegpmTypeAssujettissement> buildMapping() {
		final Map<Integer, RegpmTypeAssujettissement> map = new HashMap<>();
		map.put(1, RegpmTypeAssujettissement.LILIC);
		map.put(2, RegpmTypeAssujettissement.LIFD);
		map.put(3, RegpmTypeAssujettissement.SANS);
		return map;
	}

	public TypeAssujettissementUserType() {
		super(RegpmTypeAssujettissement.class, MAPPING);
	}
}
