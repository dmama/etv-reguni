package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;

public class EtablissementLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ETABLISSEMENT_ID,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_NO_IDE,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_ID_CANTONAL,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.ETABLISSEMENT_INDIVIDU_ID));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public EtablissementLoggedElement(RegpmEtablissement etablissement) {
		this.values = buildItemValues(etablissement);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegpmEtablissement etablissement) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ID, etablissement.getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_NO_IDE, etablissement.getNumeroIDE());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ID_CANTONAL, etablissement.getNumeroCantonal());
		if (etablissement.getEntreprise() != null) {
			LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_ENTREPRISE_ID, etablissement.getEntreprise().getId());
		}
		else if (etablissement.getIndividu() != null) {
			LoggedElementHelper.addValue(map, LoggedElementAttribute.ETABLISSEMENT_INDIVIDU_ID, etablissement.getIndividu().getId());
		}
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
