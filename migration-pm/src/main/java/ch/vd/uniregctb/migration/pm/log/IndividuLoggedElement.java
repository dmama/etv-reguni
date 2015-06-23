package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;

public class IndividuLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.INDIVIDU_ID,
	                                                                                                     LoggedElementAttribute.INDIVIDU_NOM,
	                                                                                                     LoggedElementAttribute.INDIVIDU_PRENOM,
	                                                                                                     LoggedElementAttribute.INDIVIDU_SEXE,
	                                                                                                     LoggedElementAttribute.INDIVIDU_DATE_NAISSANCE));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public IndividuLoggedElement(RegpmIndividu individu) {
		this.values = buildItemValues(individu);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegpmIndividu individu) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_ID, individu.getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_NOM, individu.getNom());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_PRENOM, individu.getPrenom());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_SEXE, individu.getSexe());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.INDIVIDU_DATE_NAISSANCE, individu.getDateNaissance());
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
