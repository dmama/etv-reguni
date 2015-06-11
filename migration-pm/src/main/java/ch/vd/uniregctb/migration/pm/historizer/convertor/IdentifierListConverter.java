package ch.vd.uniregctb.migration.pm.historizer.convertor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

public class IdentifierListConverter {

	/**
	 * Classe les identifiants par catégorie dans une Map offrant une liste de périodes de valeur pour chaque
	 * catégorie d'identifiant. Chaque période est recréée pour ne contenir que la valeur de l'identifiant.
	 *
	 * @param identifiers La liste de périodes d'identifiants, indifférement de la catégorie.
	 * @return La Map de périodes de valeur.
	 */
	public static Map<String, List<DateRanged<String>>> toMapOfListsOfDateRangedValues(List<DateRanged<Identifier>> identifiers) {
		// construction de la map
		final Map<String, List<DateRanged<String>>> map = identifiers.stream()
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toMap(dr -> dr.getPayload().getIdentifierCategory(),
				                          dr -> Collections.singletonList(new DateRanged<String>(dr.getDateDebut(), dr.getDateFin(), dr.getPayload().getIdentifierValue())),
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// vérification des overlaps
		map.entrySet().stream()
				.filter(entry -> entry.getValue().size() > 1)       // pas d'overlap possible s'il n'y a qu'un élément
				.forEach(entry -> {
					final List<DateRange> overlaps = DateRangeHelper.overlaps(entry.getValue());
					if (overlaps != null && !overlaps.isEmpty()) {
						throw new IllegalArgumentException("Found overlapping range in list of Identifiers of category " + entry.getKey() + ": " + DateRangeHelper.toDisplayString(overlaps.get(0)));
					}
				});

		// tout est bon
		return map;
	}
}
