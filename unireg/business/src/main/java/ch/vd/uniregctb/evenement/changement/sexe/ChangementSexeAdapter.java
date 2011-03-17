package ch.vd.uniregctb.evenement.changement.sexe;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class ChangementSexeAdapter extends GenericEvenementAdapter {

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
