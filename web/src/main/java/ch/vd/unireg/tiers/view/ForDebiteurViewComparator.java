package ch.vd.uniregctb.tiers.view;

import ch.vd.uniregctb.common.BaseComparator;

public class ForDebiteurViewComparator  extends BaseComparator<ForFiscalView> {

	public ForDebiteurViewComparator() {
		super(new String[] { "annule", "dateOuverture" },
                new Boolean[] { true, false});
	}

}