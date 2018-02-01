package ch.vd.unireg.tiers.view;

import ch.vd.unireg.common.BaseComparator;

public class ForDebiteurViewComparator  extends BaseComparator<ForFiscalView> {

	public ForDebiteurViewComparator() {
		super(new String[] { "annule", "dateOuverture" },
                new Boolean[] { true, false});
	}

}