package ch.vd.uniregctb.evenement.changement.origine;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

public class CorrectionOrigineAdapter extends GenericEvenementAdapter {

	protected CorrectionOrigineAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
