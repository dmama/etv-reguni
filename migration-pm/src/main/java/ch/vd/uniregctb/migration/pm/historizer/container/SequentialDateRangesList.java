package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Dérivée de {@link List} conservant une suite d'intervals temporels ne se chevauchant pas, de sorte
 * qu'il n'existe qu'une seule valeur pour un temps t.
 *
 * Cette exigence est garantie au moment de l'ajout de valeurs d'interval.
 *
 * Cette classe est utilisée pour repésenter une valeur simple journalisée
 * au moyen d'une liste de période de temps.
 *
 * @param <T> Le type de la valeur stockée.
 */
public class SequentialDateRangesList<T> extends ArrayList<DateRanged<T>> {


	/**
	 * @param values La liste de période de temps et les valeurs associées.
	 */
	public SequentialDateRangesList(List<DateRanged<T>> values) {
		super(new ArrayList<>());
		values.stream().sorted(DateRangeComparator::compareRanges).forEach(this::add);
	}

	@Override
	public boolean add(DateRanged<T> value) {
		checkOverlap(value);
		return super.add(value);
	}

	@Override
	public boolean addAll(Collection<? extends DateRanged<T>> value) {
		value.stream().forEach(this::add);
		return true;
	}

	private void checkOverlap(DateRanged<T> value) {
		if (this.size() > 0) {
			DateRanged<T> previous = this.get(this.size() - 1);
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
