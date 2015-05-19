package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Repésente une valeur simple journalisée au moyen d'une liste de période de temps.
 *
 * Les périodes de temps ne se chevauchent pas et une seule valeur existe à un temps t.
 *
 * @param <T>
 */
public class SingleValueDateRanges<T> extends ValuesDateRanges<T> {


	/**
	 * @param values La liste de période de temps et les valeurs associées.
	 */
	public SingleValueDateRanges(List<DateRanged<T>> values) {
		super(new ArrayList<>());
		values.stream().sorted(DateRangeComparator::compareRanges).forEach(this::addNext);
	}

	private void addNext(DateRanged<T> value) {
		checkOverlap(value);
		getValues().add(value);
	}

	private void checkOverlap(DateRanged<T> value) {
		final List<DateRanged<T>> values = getValues();
		if (values.size() > 0) {
			DateRanged<T> previous = values.get(values.size() - 1);
			if (DateRangeHelper.intersect(previous, value)) {
				errorOverlap(value, previous);
			}
		}
	}

	private void errorOverlap(DateRanged<T> newValue, DateRanged<T> previousValue) {
		StringBuilder errorMessage =
				new StringBuilder("-----------------------------------------------------\nEssai d'ajouter une période chevauchant la précédente:\n");
		errorMessage.append(String.format("<<           %s -> %s :\n%s\n",
		                                  RegDateHelper.dateToDisplayString(previousValue.getDateDebut()),
		                                  RegDateHelper.dateToDisplayString(previousValue.getDateFin()),
		                                  previousValue.getPayload()));
		errorMessage.append("-----------------------------------------------------\n");
		errorMessage.append(String.format(">>  CONFLIT: %s -> %s :\n%s\n",
		                                  RegDateHelper.dateToDisplayString(newValue.getDateDebut()),
		                                  RegDateHelper.dateToDisplayString(newValue.getDateFin()),
		                                  newValue.getPayload()));
		errorMessage.append("-----------------------------------------------------\n");
		throw new IllegalArgumentException(errorMessage.toString());
	}
}
