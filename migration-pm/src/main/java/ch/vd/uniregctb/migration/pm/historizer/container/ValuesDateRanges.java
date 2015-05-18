package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.List;

/**
 * Repésente une ou plusieurs valeurs journalisées au moyen d'une liste de période de temps.
 *
 * Plusieurs valeurs peuvent coexister à un temps t.
 *
 * @param <T>
 */
public class ValuesDateRanges<T> {
	private List<DateRanged<T>> values;

	public ValuesDateRanges(List<DateRanged<T>> values) {
		this.values = values;
	}
}
