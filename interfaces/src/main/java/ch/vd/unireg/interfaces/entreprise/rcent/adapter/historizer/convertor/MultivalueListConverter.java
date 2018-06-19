package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.convertor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;

public class MultivalueListConverter {

	/**
	 * Crée une Map à partir d'une liste de Ranged, étant entendu que la liste est multivaleur, c'est à
	 * dire qu'elle matérialise l'historique d'une liste.
	 *
	 * @param multiValues La liste de périodes a multiples valeurs.
	 * @return La Map de périodes de valeur.
	 */
	public static <D, K, V> Map<K, List<DateRangeHelper.Ranged<V>>> toMapOfListsOfDateRangedValues(List<DateRangeHelper.Ranged<D>> multiValues,
	                                                                                               Function<D, K> keyExtractor,
	                                                                                               Function<D, V> valueExtractor) {
		// construction de la map
		final Map<K, List<DateRangeHelper.Ranged<V>>> map = multiValues.stream()
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toMap(dr -> keyExtractor.apply(dr.getPayload()),
				                          dr -> Collections.singletonList(DateRangedConvertor.map(dr, valueExtractor)),
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// vérification des overlaps
		map.entrySet().stream()
				.filter(entry -> entry.getValue().size() > 1)       // pas d'overlap possible s'il n'y a qu'un élément
				.forEach(entry -> {
					final List<DateRange> overlaps = DateRangeHelper.overlaps(entry.getValue());
					if (overlaps != null && !overlaps.isEmpty()) {
						throw new IllegalArgumentException("Found overlapping range in list for key " + entry.getKey() + ": " + DateRangeHelper.toDisplayString(overlaps.get(0)));
					}
				});

		// tout est bon
		return map;
	}
}
