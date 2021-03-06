package ch.vd.unireg.decision.aci;

import java.util.Comparator;

import ch.vd.unireg.tiers.DecisionAciView;

public class DecisionAciViewComparator implements Comparator<DecisionAciView> {

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
	public int compare(DecisionAciView o1, DecisionAciView o2) {
		int compare = Boolean.compare(o1.isAnnule(), o2.isAnnule());
		if (compare == 0) {
			compare = - compareNullable(o1.getDateDebut(), o2.getDateDebut(), false);
		}
		return compare;
	}
}