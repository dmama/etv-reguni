package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Comparator;

/**
 * Comparateur pour ordonner les evenements dans leur ordre de traitement:
 *  1. les plus anciens en premier
 *  2. Suivant la priorité de l'événement pour les evenements ayant la meme date
 */
public class EvenementCivilEchBasicInfoComparator implements Comparator<EvenementCivilEchBasicInfo> {
	private static final TypeEvenementCivilEchComparator PRIORITY_COMPARATOR = new TypeEvenementCivilEchComparator();
	@Override
	public int compare(EvenementCivilEchBasicInfo o1, EvenementCivilEchBasicInfo o2) {
		int comp = o1.getDate().compareTo(o2.getDate());
		if (comp == 0) {
			comp = PRIORITY_COMPARATOR.compare(o1.getType(), o2.getType());
		}
		return comp;
	}
}
