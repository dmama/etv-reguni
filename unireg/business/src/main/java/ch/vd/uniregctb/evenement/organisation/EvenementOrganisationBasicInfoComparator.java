package ch.vd.uniregctb.evenement.organisation;

import java.util.Comparator;

/**
 * Comparateur pour ordonner les evenements dans leur ordre de traitement:
 *  1. les plus anciens en premier
 *  2. Puis par identifiant croissant
 */
public class EvenementOrganisationBasicInfoComparator implements Comparator<EvenementOrganisationBasicInfo> {

	@Override
	public int compare(EvenementOrganisationBasicInfo o1, EvenementOrganisationBasicInfo o2) {
		int comp = o1.getDate().compareTo(o2.getDate());
		if (comp == 0) {
			comp = (o1.getId() < o2.getId() ? -1 : (o1.getId() > o2.getId() ? 1 : 0));
		}
		return comp;
	}
}
