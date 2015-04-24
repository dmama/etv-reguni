package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.type.Sexe;

public class SexeUserType extends EnumCharMappingUserType<Sexe> {

	private static final Map<String, Sexe> MAPPING = buildMapping();

	private static Map<String, Sexe> buildMapping() {
		final Map<String, Sexe> map = new HashMap<>();
		map.put("M", Sexe.MASCULIN);
		map.put("F", Sexe.FEMININ);
		return map;
	}

	public SexeUserType() {
		super(Sexe.class, MAPPING);
	}
}
