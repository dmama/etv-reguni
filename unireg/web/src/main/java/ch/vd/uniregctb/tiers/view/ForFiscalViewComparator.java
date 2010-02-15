package ch.vd.uniregctb.tiers.view;

import ch.vd.uniregctb.common.BaseComparator;

public class ForFiscalViewComparator  extends BaseComparator<ForFiscalView> {

	public ForFiscalViewComparator() {
		super(new String[] { "annule", "genreImpot", "motifRattachement", "dateOuverture", "dateEvenement" },
                new Boolean[] { true, true, true, false, false});
	}

}