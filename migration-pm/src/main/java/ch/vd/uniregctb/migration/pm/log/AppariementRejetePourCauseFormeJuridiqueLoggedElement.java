package ch.vd.uniregctb.migration.pm.log;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;

public class AppariementRejetePourCauseFormeJuridiqueLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Collections.singletonList(LoggedElementAttribute.FORME_JURIDIQUE_RCENT));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public AppariementRejetePourCauseFormeJuridiqueLoggedElement(FormeLegale rcentFormeJuridique) {
		this.values = buildItemValues(rcentFormeJuridique);
	}

	private static Map<LoggedElementAttribute, Object> buildItemValues(FormeLegale rcentFormeJuridique) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_RCENT, rcentFormeJuridique);
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
