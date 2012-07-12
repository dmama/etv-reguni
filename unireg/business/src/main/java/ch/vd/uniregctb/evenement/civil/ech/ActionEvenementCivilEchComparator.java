package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Comparator;

import ch.vd.uniregctb.type.ActionEvenementCivilEch;

/**
 *
 * Comparteur sur la priorité d'une action  pour un evenement civil ech
 *
 */
class ActionEvenementCivilEchComparator implements Comparator<ActionEvenementCivilEch> {

	@Override
	public int compare(ActionEvenementCivilEch o1, ActionEvenementCivilEch o2) {
		return (o1.getPriorite() - o2.getPriorite());
	}

}
