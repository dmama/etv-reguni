package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeFusion;

public class FormeFusionUserType extends EnumCharMappingUserType<RegpmFormeFusion> {

	private static final Map<String, RegpmFormeFusion> MAPPING = buildMapping();

	private static Map<String, RegpmFormeFusion> buildMapping() {
		final Map<String, RegpmFormeFusion> map = new HashMap<>();
		map.put("A", RegpmFormeFusion.PAR_ANNEXION);
		map.put("B", RegpmFormeFusion.PAR_ABSORPTION);
		map.put("C", RegpmFormeFusion.PAR_COMBINAISON);
		return map;
	}

	public FormeFusionUserType() {
		super(RegpmFormeFusion.class, MAPPING);
	}
}
