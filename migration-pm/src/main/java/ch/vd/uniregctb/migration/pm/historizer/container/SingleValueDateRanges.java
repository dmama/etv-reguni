package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDateHelper;

public class SingleValueDateRanges<T> {
	private List<DateRanged<T>> values;

	public SingleValueDateRanges(List<DateRanged<T>> values) {
		this.values = new ArrayList<>(values.stream().sorted(DateRangeComparator::compareRanges).collect(Collectors.toList()));
		ensureConsecutive();
	}

	private void ensureConsecutive() {
		boolean consecutive = false;
		// Todo: implement check
		consecutive = true;
		if (!consecutive) {
			StringBuilder errorMessage = new StringBuilder("Ranges overlap in given list of date ranges:\n");
			values.stream().map(
					val -> String.format("%s -> %s",
					                     RegDateHelper.dateToDisplayString(val.getDateDebut()),
					                     RegDateHelper.dateToDisplayString(val.getDateFin())))
					.forEach(errorMessage::append);

			throw new RuntimeException(errorMessage.toString());
		}
	}
}
