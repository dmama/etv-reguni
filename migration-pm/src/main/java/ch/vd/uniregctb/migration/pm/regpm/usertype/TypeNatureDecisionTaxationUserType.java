package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeNatureDecisionTaxation;

public class TypeNatureDecisionTaxationUserType extends EnumIntegerMappingUserType<RegpmTypeNatureDecisionTaxation> {

	private static final Map<Integer, RegpmTypeNatureDecisionTaxation> MAPPING = buildMapping();

	private static Map<Integer, RegpmTypeNatureDecisionTaxation> buildMapping() {
		final Map<Integer, RegpmTypeNatureDecisionTaxation> map = new HashMap<>();
		map.put(1, RegpmTypeNatureDecisionTaxation.DEFINITIVE);
		map.put(2, RegpmTypeNatureDecisionTaxation.PROVISOIRE);
		map.put(3, RegpmTypeNatureDecisionTaxation.TAXATION_OFFICE_DEFAUT_PIECES);
		map.put(4, RegpmTypeNatureDecisionTaxation.TAXATION_OFFICE_DEFAUT_DOSSIER);
		return map;
	}

	public TypeNatureDecisionTaxationUserType() {
		super(RegpmTypeNatureDecisionTaxation.class, MAPPING);
	}

}
