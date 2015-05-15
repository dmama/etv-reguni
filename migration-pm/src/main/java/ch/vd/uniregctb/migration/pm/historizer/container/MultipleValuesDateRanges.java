package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.List;

public class MultipleValuesDateRanges<T> {
	private List<DateRanged<T>> values;

	public MultipleValuesDateRanges(List<DateRanged<T>> values) {
		this.values = values;
	}
}
