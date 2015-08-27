package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.uniregctb.common.CollectionsUtils;

/**
 * @author Raphaël Marmier, 2015-08-27
 */
public class MockUtil {

	/**
	 * Ajoute une valeur à la liste correspondant à une clé de la map. Si le Range
	 * précédant était ouvert, il est fermé à la veille. S'il est fermé à une date
	 * ultérieure, une exception est renvoyée.
	 *
	 * @param valueMap Une map de listes. Si null, une nouvelle map sera créé en place.
	 * @param key Une clé pour une liste de la map
	 * @param newRange La valeur à insérer
	 * @param <K> Le type de la clé
	 * @param <V> Le type de la valeur
	 * @return La map de liste modifiée ou créée dans l'opération
	 */
	public static <K,V> Map<K, List<DateRanged<V>>> addValueToMapOfRanges(@Nullable Map<K, List<DateRanged<V>>> valueMap,
	                                                                      @NotNull final K key,
	                                                                      @NotNull final DateRanged<V> newRange) {
		if (valueMap == null) {
			valueMap = new HashMap<>();
		}
		List<DateRanged<V>> rangeList = valueMap.get(key);
		if (rangeList == null) {
			rangeList = new ArrayList<>();
			valueMap.put(key, rangeList);
		} else {
			terminateLastRangeWithNewRange(newRange, rangeList);
		}
		rangeList.add(newRange);
		return valueMap;
	}

	private static <V> void terminateLastRangeWithNewRange(@NotNull DateRanged<V> newRange, List<DateRanged<V>> rangeList) {
		DateRanged<V> lastElement = CollectionsUtils.getLastElement(rangeList);
		if (lastElement != null) {
			if (lastElement.getDateFin() != null) {
				if (lastElement.getDateFin().isAfterOrEqual(newRange.getDateDebut())) {
					panic();
				}
			} else {
				RegDate oneDayBeforeNew = newRange.getDateDebut().getOneDayBefore();
				if (lastElement.getDateDebut().isAfter(oneDayBeforeNew)) {
					panic();
				}
				DateRanged<V> replacedLast = lastElement.withDateFin(oneDayBeforeNew);
				rangeList.remove(lastElement);
				rangeList.add(replacedLast);
			}
		}
	}

	private static void panic() {
		throw new RuntimeException("Tentative d'ajouter un Range commençant au jour ou avant le jour de fin du Range précédant!");
	}

	/**
	 * Ajoute une valeur à la liste. Si le Range précédant était ouvert, il est fermé
	 * à la veille. S'il est fermé à une date ultérieure, une exception est renvoyée.
	 *
	 * @param rangeList Une liste. Si null, une nouvelle liste sera créé en place.
	 * @param newRange La valeur à insérer
	 * @param <T> Le type de la valeur
	 * @return La liste modifiée ou créée dans l'opération
	 */
	public static <T> List<DateRanged<T>> addValueToListOfRanges(@Nullable List<DateRanged<T>> rangeList, @NotNull DateRanged<T> newRange) {
		if (rangeList == null) {
			rangeList = new ArrayList<>();
		} else {
			terminateLastRangeWithNewRange(newRange, rangeList);
		}
		rangeList.add(newRange);
		return rangeList;
	}
}
