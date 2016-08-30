package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeCollectivite;

public class CodeCollectiviteUserType extends EnumCharMappingUserType<RegpmCodeCollectivite> {

	private static final Map<String, RegpmCodeCollectivite> MAPPING = buildMapping();

	private static Map<String, RegpmCodeCollectivite> buildMapping() {
		final Map<String, RegpmCodeCollectivite> map = new HashMap<>();
		map.put("CO", RegpmCodeCollectivite.COMMUNE);
		map.put("CT", RegpmCodeCollectivite.CANTON);
		map.put("CH", RegpmCodeCollectivite.CONFEDERATION);
		return map;
	}

	public CodeCollectiviteUserType() {
		super(RegpmCodeCollectivite.class, MAPPING);
	}
}
