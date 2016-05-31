package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;

public class FormesJuridiquesIncompatiblesLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.FORME_JURIDIQUE_REGPM,
	                                                                                                     LoggedElementAttribute.FORME_JURIDIQUE_RCENT,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_REGPM,
	                                                                                                     LoggedElementAttribute.RAISON_SOCIALE_RCENT));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public FormesJuridiquesIncompatiblesLoggedElement(String regpmFormeJuridique, FormeLegale rcentFormeJuridique, String regpmRaisonSociale, String rcentRaisonSociale) {
		this.values = buildItemValues(regpmFormeJuridique, rcentFormeJuridique, regpmRaisonSociale, rcentRaisonSociale);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(String regpmFormeJuridique, FormeLegale rcentFormeJuridique, String regpmRaisonSociale, String rcentRaisonSociale) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_REGPM, regpmFormeJuridique);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.FORME_JURIDIQUE_RCENT, rcentFormeJuridique);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_REGPM, regpmRaisonSociale);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.RAISON_SOCIALE_RCENT, rcentRaisonSociale);
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
