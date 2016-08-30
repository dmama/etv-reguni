package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class AppariementEtablissementLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.RAISON_SOCIALE_REGPM,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_RCENT,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_ID_CANTONAL));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public AppariementEtablissementLoggedElement(long idCantonalEtablissment, String regpmRaisonSociale, String rcentRaisonSociale) {
		this.values = buildItemValues(idCantonalEtablissment, regpmRaisonSociale, rcentRaisonSociale);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(long idCantonalEtablissment, String regpmRaisonSociale, String rcentRaisonSociale) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_REGPM, regpmRaisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_RCENT, rcentRaisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ID_CANTONAL, idCantonalEtablissment);
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
