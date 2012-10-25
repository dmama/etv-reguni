package ch.vd.uniregctb.tiers.view;

import java.util.Comparator;

/**
 * Compare les vues sur les periodicites compte tenu de l'algorithme de tri suivant:
 * <ul>
 * <li>Annulé / pas annulé (annulé toujours après)</li>
 * <li>Date d'ouverture décroissante de la periodicite</li>
 * </ul>
 */
public class PeriodiciteViewComparator implements Comparator<PeriodiciteView> {


	private static <T extends Comparable<T>> int compareNullable(T o1, T o2, boolean nullAtEnd) {
		if (o1 == o2) {
			return 0;
		}
		else if (o1 == null) {
			return nullAtEnd ? -1 : 1;
		}
		else if (o2 == null) {
			return nullAtEnd ? 1 : -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}



	@Override
	public int compare(PeriodiciteView o1, PeriodiciteView o2) {
		int compare = Boolean.valueOf(o1.isAnnule()).compareTo(o2.isAnnule());	
		if (compare == 0) {
			compare = - compareNullable(o1.getDateDebut(), o2.getDateDebut(), false);
		}
		return compare;
	}
}