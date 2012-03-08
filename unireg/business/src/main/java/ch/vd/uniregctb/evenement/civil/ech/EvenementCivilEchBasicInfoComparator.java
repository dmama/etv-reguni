package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Comparator;

// TODO FRED Javadoc expliquer la logique du tri
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
