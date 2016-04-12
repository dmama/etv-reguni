package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmFonction;

public class FonctionUserType extends EnumCharMappingUserType<RegpmFonction> {

	private static final Map<String, RegpmFonction> MAPPING = buildMapping();

	private static Map<String, RegpmFonction> buildMapping() {
		final Map<String, RegpmFonction> map = new HashMap<>();
		map.put("01", RegpmFonction.PRESIDENT);
		map.put("02", RegpmFonction.VICE_PRESIDENT);
		map.put("03", RegpmFonction.SECRETAIRE);
		map.put("04", RegpmFonction.TRESORIER);
		map.put("05", RegpmFonction.CAISSIER);
		map.put("06", RegpmFonction.ASSOCIE_INDEF_RESP);
		map.put("07", RegpmFonction.COMMANDITAIRE);
		map.put("08", RegpmFonction.ASSOCIE_GERANT);
		map.put("09", RegpmFonction.ASSOCIE);
		map.put("10", RegpmFonction.RESPONSABLE_SUCCURSALE);
		map.put("11", RegpmFonction.ADMINISTRATEUR);
		map.put("12", RegpmFonction.CURATEUR);
		map.put("13", RegpmFonction.DIRECTEUR);
		map.put("14", RegpmFonction.SOUS_DIRECTEUR);
		map.put("15", RegpmFonction.LIQUIDATEUR);
		return map;
	}

	public FonctionUserType() {
		super(RegpmFonction.class, MAPPING);
	}
}
