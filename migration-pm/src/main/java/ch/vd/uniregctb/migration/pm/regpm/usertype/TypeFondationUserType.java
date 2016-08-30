package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFondation;

public class TypeFondationUserType extends EnumCharMappingUserType<RegpmTypeFondation> {

	private static final Map<String, RegpmTypeFondation> MAPPING = buildMapping();

	private static Map<String, RegpmTypeFondation> buildMapping() {
		final Map<String, RegpmTypeFondation> map = new HashMap<>();
		map.put("Ecclésiastique", RegpmTypeFondation.ECCLESIASTIQUE);
		map.put("Famille", RegpmTypeFondation.FAMILLE);
		map.put("Fondation", RegpmTypeFondation.FONDATION);
		map.put("Prévoyance", RegpmTypeFondation.PREVOYANCE);
		return map;
	}

	public TypeFondationUserType() {
		super(RegpmTypeFondation.class, MAPPING);
	}
}
