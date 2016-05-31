package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseIndividu;

public class TypeAdresseIndividuUserType extends EnumCharMappingUserType<RegpmTypeAdresseIndividu> {

	private static final Map<String, RegpmTypeAdresseIndividu> MAPPING = buildMapping();

	private static Map<String, RegpmTypeAdresseIndividu> buildMapping() {
		final Map<String, RegpmTypeAdresseIndividu> map = new HashMap<>();
		map.put("C", RegpmTypeAdresseIndividu.COURRIER);
		map.put("P", RegpmTypeAdresseIndividu.PRINCIPALE);
		map.put("S", RegpmTypeAdresseIndividu.SECONDAIRE);
		map.put("T", RegpmTypeAdresseIndividu.TUTELLE);
		return map;
	}

	public TypeAdresseIndividuUserType() {
		super(RegpmTypeAdresseIndividu.class, MAPPING);
	}
}
