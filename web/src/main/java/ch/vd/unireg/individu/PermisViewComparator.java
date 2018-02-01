package ch.vd.unireg.individu;

import ch.vd.unireg.common.BaseComparator;

public class PermisViewComparator extends BaseComparator<PermisView>{
	public PermisViewComparator() {
		super(new String[] { "annule", "dateDebut" },
                new Boolean[] { true,  false});
	}
}

