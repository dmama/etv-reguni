package ch.vd.uniregctb.evenement.common;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class EvenementCivilIgnoreAdapter extends GenericEvenementAdapter {

	protected EvenementCivilIgnoreAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	public boolean isContribuablePresentBefore() {
		return false;
	}

}
