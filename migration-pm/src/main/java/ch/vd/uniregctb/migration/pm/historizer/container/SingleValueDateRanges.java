package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Repésente une valeur simple journalisée au moyen d'une liste de période de temps.
 *
 * Les périodes de temps ne se chevauchent pas et une seule valeur existe à un temps t.
 *
 * @param <T> La liste de période de temps et les valeurs associées.
 */
public class SingleValueDateRanges<T> extends ValuesDateRanges<T> {
	private List<DateRanged<T>> values = new ArrayList<>();

	public SingleValueDateRanges(List<DateRanged<T>> values) {
		super(values);
		values.stream().sorted(DateRangeComparator::compareRanges).collect(Collectors.toList()).forEach(this::addNext);
	}

	private void addNext(DateRanged<T> value) {
		checkOverlap(value);
		values.add(value);
	}

	private void checkOverlap(DateRanged<T> value) {
		if (values.size() > 0) {
			DateRanged<T> previous = values.get(values.size() - 1);
			if (previous.getDateFin() != null) {
				if (previous.getDateFin().compareTo(value.getDateDebut()) >= 0) {
					errorOverlap(value, previous);
				}
			} else {
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
		throw new RuntimeException(errorMessage.toString());
	}
}
