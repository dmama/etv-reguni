package ch.vd.uniregctb.evenement.changement.conjoint;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

public class CorrectionConjointAdapter extends GenericEvenementAdapter {

	protected CorrectionConjointAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
