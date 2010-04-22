package ch.vd.uniregctb.evenement.common;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class EvenementCivilIgnoreAdapter extends GenericEvenementAdapter {

	@Override
	public boolean isContribuablePresentBefore() {
		return false;
	}

}
