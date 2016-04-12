package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeGroupeProprietaire;

public class TypeGroupeProprietaireUserType extends EnumIntegerMappingUserType<RegpmTypeGroupeProprietaire> {

	private static final Map<Integer, RegpmTypeGroupeProprietaire> MAPPING = buildMapping();

	private static Map<Integer, RegpmTypeGroupeProprietaire> buildMapping() {
		final Map<Integer, RegpmTypeGroupeProprietaire> map = new HashMap<>();
		map.put(1, RegpmTypeGroupeProprietaire.CONSORTIUM_SOCIETE_SIMPLE);
		map.put(2, RegpmTypeGroupeProprietaire.HOIRIE);
		return map;
	}

	public TypeGroupeProprietaireUserType() {
		super(RegpmTypeGroupeProprietaire.class, MAPPING);
	}
}
