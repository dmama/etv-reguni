package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.regpm.AdresseAvecRue;

public class AdresseLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ADRESSE_RUE,
	                                                                                                     LoggedElementAttribute.ADRESSE_NO_POLICE,
	                                                                                                     LoggedElementAttribute.ADRESSE_LIEU,
	                                                                                                     LoggedElementAttribute.ADRESSE_LOCALITE,
	                                                                                                     LoggedElementAttribute.ADRESSE_PAYS));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public AdresseLoggedElement(AdresseAvecRue adresse) {
		this.values = buildItemValues(adresse);
	}

	private static String extractNomRue(AdresseAvecRue adresse) {
		return adresse.getRue() != null ? adresse.getRue().getDesignationCourrier() : adresse.getNomRue();
	}

	private static String extractLocalitePostale(AdresseAvecRue adresse) {
		return adresse.getRue() != null
				? adresse.getRue().getLocalitePostale().getNomLong()
				: (adresse.getLocalitePostale() != null ? adresse.getLocalitePostale().getNomLong() : null);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(AdresseAvecRue adresse) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_RUE, extractNomRue(adresse));
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_NO_POLICE, adresse.getNoPolice());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_LIEU, adresse.getLieu());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_LOCALITE, extractLocalitePostale(adresse));
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_PAYS, adresse.getOfsPays());
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
