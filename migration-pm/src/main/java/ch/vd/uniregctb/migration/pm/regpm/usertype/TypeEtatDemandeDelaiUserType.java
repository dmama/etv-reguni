package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;

public class TypeEtatDemandeDelaiUserType extends EnumCharMappingUserType<RegpmTypeEtatDemandeDelai> {

	private static final Map<String, RegpmTypeEtatDemandeDelai> MAPPING = buildMapping();

	private static Map<String, RegpmTypeEtatDemandeDelai> buildMapping() {
		final Map<String, RegpmTypeEtatDemandeDelai> map = new HashMap<>();
		map.put("A", RegpmTypeEtatDemandeDelai.ACCORDEE);
		map.put("D", RegpmTypeEtatDemandeDelai.DEMANDEE);
		map.put("R", RegpmTypeEtatDemandeDelai.REFUSEE);
		return map;
	}

	public TypeEtatDemandeDelaiUserType() {
		super(RegpmTypeEtatDemandeDelai.class, MAPPING);
	}
}
