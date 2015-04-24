package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseEntreprise;

public class TypeAdresseEntrepriseUserType extends EnumCharMappingUserType<RegpmTypeAdresseEntreprise> {

	private static final Map<String, RegpmTypeAdresseEntreprise> MAPPING = buildMapping();

	private static Map<String, RegpmTypeAdresseEntreprise> buildMapping() {
		final Map<String, RegpmTypeAdresseEntreprise> map = new HashMap<>();
		map.put("C", RegpmTypeAdresseEntreprise.COURRIER);
		map.put("F", RegpmTypeAdresseEntreprise.FACTURATION);
		map.put("S", RegpmTypeAdresseEntreprise.SIEGE);
		return map;
	}

	public TypeAdresseEntrepriseUserType() {
		super(RegpmTypeAdresseEntreprise.class, MAPPING);
	}
}
