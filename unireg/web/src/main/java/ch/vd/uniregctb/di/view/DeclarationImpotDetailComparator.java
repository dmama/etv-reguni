package ch.vd.uniregctb.di.view;

import ch.vd.uniregctb.common.BaseComparator;

public class DeclarationImpotDetailComparator extends BaseComparator<DeclarationImpotDetailView> {

	public DeclarationImpotDetailComparator() {
		super(new String[] { "annule", "dateDebutPeriodeImposition" },
                new Boolean[] { true, false});
	}

}