package ch.vd.uniregctb.individu;

import ch.vd.uniregctb.common.BaseComparator;

public class PermisViewComparator extends BaseComparator<PermisView>{
	public PermisViewComparator() {
		super(new String[] { "annule", "dateDebut" },
                new Boolean[] { true,  false});
	}
}

