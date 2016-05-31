package ch.vd.uniregctb.migration.pm.log;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public final class SimpleLoggedElement<T> implements LoggedElement {

	private final LoggedElementAttribute item;
	private final T value;

	public SimpleLoggedElement(LoggedElementAttribute item, T value) {
		LoggedElementHelper.checkValue(item, value);
		this.item = item;
		this.value = value;
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return Collections.singletonList(item);
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		return Collections.singletonMap(item, value);
	}
}
