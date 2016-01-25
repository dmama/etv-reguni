package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.RegimeFiscal;

public class RegimeFiscalMappingLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.DATE_DEBUT_RF,
	                                                                                                     LoggedElementAttribute.PORTEE_RF,
	                                                                                                     LoggedElementAttribute.ANCIEN_CODE_RF,
	                                                                                                     LoggedElementAttribute.NOUVEAU_CODE_RF));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public RegimeFiscalMappingLoggedElement(RegimeFiscal rf, String codeSource) {
		this.values = buildItemValues(rf, codeSource);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegimeFiscal rf, String codeSource) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.DATE_DEBUT_RF, rf.getDateDebut());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.PORTEE_RF, rf.getPortee());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ANCIEN_CODE_RF, codeSource);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.NOUVEAU_CODE_RF, rf.getCode());
		return Collections.unmodifiableMap(map);
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return NAMES;
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		return values;
	}
}
