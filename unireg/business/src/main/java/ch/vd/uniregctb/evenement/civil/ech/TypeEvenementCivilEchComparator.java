package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Comparator;

import ch.vd.uniregctb.type.TypeEvenementCivilEch;

// TODO FRED Javadoc expliquer la logique du tri
public class TypeEvenementCivilEchComparator implements Comparator<TypeEvenementCivilEch> {
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
			comp = myPrio - otherPrio;
		}
		return comp;
	}
}
