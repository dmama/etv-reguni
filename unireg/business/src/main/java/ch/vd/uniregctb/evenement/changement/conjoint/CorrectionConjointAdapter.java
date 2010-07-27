package ch.vd.uniregctb.evenement.changement.conjoint;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class CorrectionConjointAdapter extends GenericEvenementAdapter {

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
