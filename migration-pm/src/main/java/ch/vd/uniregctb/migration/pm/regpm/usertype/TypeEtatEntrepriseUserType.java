package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatEntreprise;

public class TypeEtatEntrepriseUserType extends EnumCharMappingUserType<RegpmTypeEtatEntreprise> {

	private static final Map<String, RegpmTypeEtatEntreprise> MAPPING = buildMapping();

	private static Map<String, RegpmTypeEtatEntreprise> buildMapping() {
		final Map<String, RegpmTypeEtatEntreprise> map = new HashMap<>();
		map.put("01", RegpmTypeEtatEntreprise.INSCRITE_AU_RC);
		map.put("02", RegpmTypeEtatEntreprise.EN_LIQUIDATION);
		map.put("03", RegpmTypeEtatEntreprise.EN_SUSPENS_FAILLITE);
		map.put("04", RegpmTypeEtatEntreprise.EN_FAILLITE);
		map.put("05", RegpmTypeEtatEntreprise.ABSORBEE);
		map.put("06", RegpmTypeEtatEntreprise.RADIEE_DU_RC);
		map.put("07", RegpmTypeEtatEntreprise.FONDEE);
		map.put("08", RegpmTypeEtatEntreprise.DISSOUTE);
		return map;
	}

	public TypeEtatEntrepriseUserType() {
		super(RegpmTypeEtatEntreprise.class, MAPPING);
	}
}
