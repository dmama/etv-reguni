package ch.vd.unireg.evenement.civil.ech;

import java.util.Comparator;

import ch.vd.unireg.type.TypeEvenementCivilEch;

/**
 * Comparateur sur la priorité entre les types d'evenement.<br/>
 * Utiliser pour determiner l'ordre de traitement des evenements d'un même individu.
 */

class TypeEvenementCivilEchComparator implements Comparator<TypeEvenementCivilEch> {
	@Override
	public int compare(TypeEvenementCivilEch o1, TypeEvenementCivilEch o2) {
		final Integer myPrio = o1.getPriorite();
		final Integer otherPrio = o2.getPriorite();
		final int comp;
		if (myPrio == null) {
			comp = (otherPrio == null ? 0 : 1);
		}
		else if (otherPrio == null) {
			comp = -1;
		}
		else {
			comp = Integer.compare(myPrio, otherPrio);
		}
		return comp;
	}
}
