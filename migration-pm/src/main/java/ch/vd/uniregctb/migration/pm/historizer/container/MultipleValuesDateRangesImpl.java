package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.List;

/**
 * Repésente une valeur simple journalisée au moyen d'une liste de période de temps.
 *
 * De multiples valeurs peuvent cohabiter et se chevaucher dans le temps.
 *
 * @param <T>
 */
public class MultipleValuesDateRangesImpl<T> extends ValuesDateRanges<T> {

	/**
	 * @param values La liste de période de temps et les valeurs associées.
	 */
	public MultipleValuesDateRangesImpl(List<DateRanged<T>> values) {
		super(values);
	}
}
