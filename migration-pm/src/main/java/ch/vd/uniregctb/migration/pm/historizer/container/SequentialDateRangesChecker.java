package ch.vd.uniregctb.migration.pm.historizer.container;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Outil permettant de vérifier qu'une suite d'intervales temporels ne se chevauchent pas,
 * de sorte qu'il n'existe qu'une seule valeur pour un temps t.
 *
 * @param <T>
 */
public class SequentialDateRangesChecker{

	/**
	 * Vérifie la séquentialité d'une liste de période de temps.
	 *
	 * La liste peut présenter des trous, mais aucune période ne peut chevaucher une autre.
	 *
	 * @param values La liste de période de temps.
	 * @param <T> Le type de la valeur payload de chaque {@link DateRanged}&lt;T&gt;.
	 */
	public static <T> void ensureSequential(List<DateRanged<T>> values) {
		if (values.size() > 1) { // On contrôle à partir de la présence d'au moins deux périodes.
			List<DateRanged<T>> data = new ArrayList<>(values.size());
			values.stream().sorted(DateRangeComparator::compareRanges).forEach(data::add);
			for (int i = 1; i < data.size(); i++) {
				final DateRanged<T> previous = data.get(i - 1);
				final DateRanged<T> next = data.get(i);
				if (DateRangeHelper.intersect(previous, next)) {
					errorOverlap(next, previous);
				}
			}
		}
	}


	/**
	 * Vérifie la séquentialité d'une liste de période de temps.
	 *
	 * La liste peut présenter des trous, mais aucune période ne peut chevaucher une autre.
	 *
	 * @param values Stream de périodes de temps.
	 * @param <T> Le type de la valeur payload de chaque {@link DateRanged}&lt;T&gt;.
	 */
	public static <T> void ensureSequential(Stream<DateRanged<T>> values) {
		List<DateRanged<T>> data = new ArrayList<>();
		values.sorted(DateRangeComparator::compareRanges).forEach(data::add);
		doCheck(data);
	}

	/**
	 * Parcoure la liste et compare deux à deux
	 * @param data
	 * @param <T>
	 */
	private static <T> void doCheck(List<DateRanged<T>> data) {
		for (int i = 1; i < data.size(); i++) {
			final DateRanged<T> previous = data.get(i - 1);
			final DateRanged<T> next = data.get(i);
			if (DateRangeHelper.intersect(previous, next)) {
				errorOverlap(next, previous);
			}
		}
	}

	private static <T> void errorOverlap(DateRanged<T> newValue, DateRanged<T> previousValue) {
		StringBuilder errorMessage =
				new StringBuilder("-----------------------------------------------------\nSéquence invalide: deux périodes se chevauchent:\n");
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
