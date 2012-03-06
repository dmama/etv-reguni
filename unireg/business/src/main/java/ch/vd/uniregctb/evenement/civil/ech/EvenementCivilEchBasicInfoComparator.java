package ch.vd.uniregctb.evenement.civil.ech;

import java.util.Comparator;

public class EvenementCivilEchBasicInfoComparator implements Comparator<EvenementCivilEchBasicInfo> {
	private static final TypeEvenementCivilEchComparator PRIORITY_COMPARATOR = new TypeEvenementCivilEchComparator();
	@Override
	public int compare(EvenementCivilEchBasicInfo o1, EvenementCivilEchBasicInfo o2) {
		int comp = o1.date.compareTo(o2.date);
		if (comp == 0) {
			comp = PRIORITY_COMPARATOR.compare(o1.type, o2.type);
		}
		return comp;
	}
}
