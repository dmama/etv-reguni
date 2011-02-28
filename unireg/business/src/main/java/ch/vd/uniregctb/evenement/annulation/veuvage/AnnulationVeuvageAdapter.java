package ch.vd.uniregctb.evenement.annulation.veuvage;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

public class AnnulationVeuvageAdapter extends GenericEvenementAdapter implements AnnulationVeuvage {

	protected AnnulationVeuvageAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
