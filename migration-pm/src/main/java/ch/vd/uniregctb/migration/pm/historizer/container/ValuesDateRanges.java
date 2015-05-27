package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.List;

/**
 * Repésente une ou plusieurs valeurs journalisées au moyen d'une liste de période de temps.
 *
 * Plusieurs valeurs peuvent coexister à un temps t.
 *
 * @param <T>
 */
public abstract class ValuesDateRanges<T> {
	private final List<DateRanged<T>> values;

	public ValuesDateRanges(List<DateRanged<T>> values) {
		this.values = values;
	}

	protected List<DateRanged<T>> getValues() {
		return this.values;
	}
}
