package ch.vd.uniregctb.evenement.organisation;

import java.util.Comparator;

/**
 * Comparateur pour ordonner les evenements dans leur ordre de traitement:
 *  1. les plus anciens en premier
 *  2. Puis par identifiant croissant
 */
public class EvenementOrganisationComparator implements Comparator<EvenementOrganisation> {

	@Override
	public int compare(EvenementOrganisation o1, EvenementOrganisation o2) {
		int comp = o1.getDateEvenement().compareTo(o2.getDateEvenement());
		if (comp == 0) {
			comp = Long.compare(o1.getId(), o2.getId());
		}
		return comp;
	}
}
