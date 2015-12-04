package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmMotifEnvoi;

public class MotifEnvoiUserType extends EnumIntegerMappingUserType<RegpmMotifEnvoi> {

	private static final Map<Integer, RegpmMotifEnvoi> MAPPING = buildMapping();

	private static Map<Integer, RegpmMotifEnvoi> buildMapping() {
		final Map<Integer, RegpmMotifEnvoi> map = new HashMap<>();
		map.put(1, RegpmMotifEnvoi.FIN_EXERCICE);
		map.put(2, RegpmMotifEnvoi.FIN_ASSUJETTISSEMENT);
		map.put(3, RegpmMotifEnvoi.TRANSFERT_SIEGE);
		return map;
	}

	public MotifEnvoiUserType() {
		super(RegpmMotifEnvoi.class, MAPPING);
	}
}
