package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.adresse.AdresseGenerique;

public class AdressePermanenteLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ADRESSE_DATE_DEBUT,
	                                                                                                     LoggedElementAttribute.ADRESSE_DATE_FIN,
	                                                                                                     LoggedElementAttribute.ADRESSE_COMPLEMENT,
	                                                                                                     LoggedElementAttribute.ADRESSE_RUE,
	                                                                                                     LoggedElementAttribute.ADRESSE_NO_POLICE,
	                                                                                                     LoggedElementAttribute.ADRESSE_NPA,
	                                                                                                     LoggedElementAttribute.ADRESSE_LOCALITE,
	                                                                                                     LoggedElementAttribute.ADRESSE_PAYS));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public AdressePermanenteLoggedElement(AdresseGenerique adresse) {
		this.values = buildItemValues(adresse);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(AdresseGenerique adresse) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_DATE_DEBUT, adresse.getDateDebut());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_DATE_FIN, adresse.getDateFin());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_COMPLEMENT, adresse.getComplement());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_RUE, adresse.getRue());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_NO_POLICE, adresse.getNumero());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_LOCALITE, adresse.getNumeroPostal());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_LOCALITE, adresse.getLocaliteComplete());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ADRESSE_PAYS, adresse.getNoOfsPays());
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
