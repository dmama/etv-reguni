package ch.vd.uniregctb.migration.pm.historizer.convertor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

public class IdentifierListConverter {

	/**
	 * Classe les identifiants par catégorie dans une Map offrant une liste de périodes de valeur pour chaque
	 * catégorie d'identifiant. Chaque période est recréée pour ne contenir que la valeur de l'identifiant.
	 *
	 * @param identifiers La liste de périodes d'identifiants, indifférament de la catégorie.
	 * @return La Map de périodes de valeur.
	 */
	public static Map<String, List<DateRanged<String>>> toMapOfListsOfDateRangedValues(List<DateRanged<Identifier>> identifiers) {
		Map<String, List<DateRanged<String>>> identifierMap = new HashMap<>();
		identifiers.sort(DateRangeComparator::compareRanges);
		for (DateRanged<Identifier> d : identifiers) {
			List<DateRanged<String>> dateRangeds = identifierMap.get(d.getPayload().getIdentifierCategory());
			if (dateRangeds == null) {
				dateRangeds = new ArrayList<>();
				identifierMap.put(d.getPayload().getIdentifierCategory(), dateRangeds);
			}
			if (dateRangeds.size() > 0) {
				final DateRanged<String> previousDateRanged = dateRangeds.get(dateRangeds.size() - 1);
				if (previousDateRanged.getDateFin() == null || d.getDateDebut().isBeforeOrEqual(previousDateRanged.getDateFin())) {
					return reportError(d, previousDateRanged);
				}
			}
			dateRangeds.add(DateRangedConvertor.convert(d, Identifier::getIdentifierValue));
		}
		return identifierMap;
	}

	private static Map<String, List<DateRanged<String>>> reportError(DateRanged<Identifier> d, DateRanged<String> previousDateRanged) {
		throw new IllegalArgumentException("Found overlapping range in list of Identifiers! \nNew range starts " +
				                                   "at: " + d.getDateDebut() + " while previous one ends " +
				                                   "at: " + previousDateRanged.getDateFin() + "\n" +
				                                   "Identifier category: " + d.getPayload().getIdentifierCategory() + " " +
				                                   "and values old: " + previousDateRanged.getPayload() + " " +
				                                   "new: " + d.getPayload().getIdentifierValue() + "\n");
	}
}
