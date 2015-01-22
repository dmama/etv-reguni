package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Comparator;

/**
 * Comparateur pour ordonner les evenements dans leur ordre de traitement:
 *  1. les plus anciens en premier
 *  2. Suivant la priorité du type de l'événement pour les evenements ayant la meme date
 *  3. Suivant la priorité de l'action pour les evenements avec un type de même priorité
 *  4. Puis par identifiant croissant
 */
public class EvenementCivilEchBasicInfoComparator implements Comparator<EvenementCivilEchBasicInfo> {

	private static final TypeEvenementCivilEchComparator TYPE_PRIORITY_COMPARATOR = new TypeEvenementCivilEchComparator();
	private static final ActionEvenementCivilEchComparator ACTION_PRIORITY_COMPARATOR = new ActionEvenementCivilEchComparator();

	@Override
	public int compare(EvenementCivilEchBasicInfo o1, EvenementCivilEchBasicInfo o2) {
		int comp = o1.getDate().compareTo(o2.getDate());
		if (comp == 0) {
			comp = TYPE_PRIORITY_COMPARATOR.compare(o1.getType(), o2.getType());
		}
		if (comp == 0) {
			comp = ACTION_PRIORITY_COMPARATOR.compare(o1.getAction(), o2.getAction());
		}
		if (comp == 0) {
			comp = (o1.getId() < o2.getId() ? -1 : (o1.getId() > o2.getId() ? 1 : 0));
		}
		return comp;
	}
}
