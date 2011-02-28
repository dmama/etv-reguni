package ch.vd.uniregctb.evenement.changement.sexe;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

public class ChangementSexeAdapter extends GenericEvenementAdapter {

	protected ChangementSexeAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
