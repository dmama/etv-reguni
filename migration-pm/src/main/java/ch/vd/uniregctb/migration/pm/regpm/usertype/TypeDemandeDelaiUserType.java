package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;

public class TypeDemandeDelaiUserType extends EnumCharMappingUserType<RegpmTypeDemandeDelai> {

	private static final Map<String, RegpmTypeDemandeDelai> MAPPING = buildMapping();

	private static Map<String, RegpmTypeDemandeDelai> buildMapping() {
		final Map<String, RegpmTypeDemandeDelai> map = new HashMap<>();
		map.put("D", RegpmTypeDemandeDelai.AVANT_SOMMATION);
		map.put("S", RegpmTypeDemandeDelai.APRES_SOMMATION);
		return map;
	}

	public TypeDemandeDelaiUserType() {
		super(RegpmTypeDemandeDelai.class, MAPPING);
	}
}
