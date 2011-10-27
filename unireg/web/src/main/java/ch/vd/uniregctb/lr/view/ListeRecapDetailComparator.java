package ch.vd.uniregctb.lr.view;

import ch.vd.uniregctb.common.BaseComparator;

public class ListeRecapDetailComparator  extends BaseComparator<ListeRecapDetailView> {

	public ListeRecapDetailComparator() {
		super(new String[] { "annule", "dateDebutPeriode" },
                new Boolean[] { true, false});
	}

}
