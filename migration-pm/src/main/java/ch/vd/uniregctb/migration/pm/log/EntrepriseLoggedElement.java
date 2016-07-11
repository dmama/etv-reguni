package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.engine.ActivityManager;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;

public class EntrepriseLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.ENTREPRISE_ID,
	                                                                                                     LoggedElementAttribute.ENTREPRISE_FLAG_ACTIF,
	                                                                                                     LoggedElementAttribute.ENTREPRISE_NO_IDE,
	                                                                                                     LoggedElementAttribute.ENTREPRISE_ID_CANTONAL));

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	private final Map<LoggedElementAttribute, Object> values;

	public EntrepriseLoggedElement(RegpmEntreprise entreprise, ActivityManager activityManager) {
		this(entreprise, activityManager.isActive(entreprise));
	}

	public EntrepriseLoggedElement(RegpmEntreprise entreprise, boolean active) {
		this.values = buildItemValues(entreprise, active);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(RegpmEntreprise entreprise, boolean active) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ENTREPRISE_ID, entreprise.getId());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ENTREPRISE_FLAG_ACTIF, active);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ENTREPRISE_NO_IDE, entreprise.getNumeroIDE());
		LoggedElementHelper.addValue(map, LoggedElementAttribute.ENTREPRISE_ID_CANTONAL, entreprise.getNumeroCantonal());
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
