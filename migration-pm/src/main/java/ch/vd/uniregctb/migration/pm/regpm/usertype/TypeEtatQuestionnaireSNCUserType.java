package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatQuestionnaireSNC;

public class TypeEtatQuestionnaireSNCUserType extends EnumCharMappingUserType<RegpmTypeEtatQuestionnaireSNC> {

	private static final Map<String, RegpmTypeEtatQuestionnaireSNC> MAPPING = buildMapping();

	private static Map<String, RegpmTypeEtatQuestionnaireSNC> buildMapping() {
		final Map<String, RegpmTypeEtatQuestionnaireSNC> map = new HashMap<>();
		map.put("ANNULE", RegpmTypeEtatQuestionnaireSNC.ANNULE);
		map.put("ENVOYE", RegpmTypeEtatQuestionnaireSNC.ENVOYE);
		map.put("RECU", RegpmTypeEtatQuestionnaireSNC.RECU);
		map.put("TAXE", RegpmTypeEtatQuestionnaireSNC.TAXE);
		return map;
	}

	public TypeEtatQuestionnaireSNCUserType() {
		super(RegpmTypeEtatQuestionnaireSNC.class, MAPPING);
	}
}
