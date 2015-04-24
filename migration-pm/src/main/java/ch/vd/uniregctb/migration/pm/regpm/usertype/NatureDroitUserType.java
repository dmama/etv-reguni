package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmNatureDroit;

public class NatureDroitUserType extends EnumCharMappingUserType<RegpmNatureDroit> {

	private static final Map<String, RegpmNatureDroit> MAPPING = buildMapping();

	private static Map<String, RegpmNatureDroit> buildMapping() {
		final Map<String, RegpmNatureDroit> map = new HashMap<>();
		map.put("Mixte", RegpmNatureDroit.MIXTE);
		map.put("Privé", RegpmNatureDroit.PRIVE);
		map.put("Public", RegpmNatureDroit.PUBLIC);
		return map;
	}

	public NatureDroitUserType() {
		super(RegpmNatureDroit.class, MAPPING);
	}
}
