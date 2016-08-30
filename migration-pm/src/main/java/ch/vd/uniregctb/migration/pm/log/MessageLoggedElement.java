package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class MessageLoggedElement implements LoggedElement {

	private static final List<LoggedElementAttribute> NAMES = Collections.unmodifiableList(Arrays.asList(LoggedElementAttribute.NIVEAU,
	                                                                                                     LoggedElementAttribute.MESSAGE));

	private final Map<LoggedElementAttribute, Object> values;

	public static final LoggedElement EMPTY = new EmptyValuedLoggedElement(NAMES);

	public MessageLoggedElement(LogLevel niveau, String message) {
		this.values = buildItemValues(niveau, message);
	}

	@NotNull
	private static Map<LoggedElementAttribute, Object> buildItemValues(LogLevel niveau, String message) {
		final Map<LoggedElementAttribute, Object> map = new EnumMap<>(LoggedElementAttribute.class);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.NIVEAU, niveau);
		LoggedElementHelper.addValue(map, LoggedElementAttribute.MESSAGE, message);
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
