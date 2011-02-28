package ch.vd.uniregctb.evenement.annulation.deces;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.deces.DecesAdapter;

public class AnnulationDecesAdapter extends DecesAdapter implements AnnulationDeces {

	protected AnnulationDecesAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
