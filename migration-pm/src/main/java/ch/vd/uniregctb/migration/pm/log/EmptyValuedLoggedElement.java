package ch.vd.uniregctb.migration.pm.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Elément qui permet de placer des "cases vides" (ou "vides jusqu'à preuve du contraire") dans un CSV
 */
public final class EmptyValuedLoggedElement implements LoggedElement {

	private final List<LoggedElementAttribute> items;

	public EmptyValuedLoggedElement(LoggedElementAttribute... items) {
		this(items == null || items.length == 0 ? Collections.emptyList() : Arrays.asList(items));
	}

	public EmptyValuedLoggedElement(List<LoggedElementAttribute> items) {
		this.items = items == null ? Collections.emptyList() : Collections.unmodifiableList(items);
	}

	@NotNull
	@Override
	public List<LoggedElementAttribute> getItems() {
		return items;
	}

	@NotNull
	@Override
	public Map<LoggedElementAttribute, Object> getItemValues() {
		return Collections.emptyMap();
	}
}
