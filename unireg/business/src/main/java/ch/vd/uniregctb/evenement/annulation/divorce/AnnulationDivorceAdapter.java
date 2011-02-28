package ch.vd.uniregctb.evenement.annulation.divorce;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.annulation.separation.AnnulationSeparationAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour l'annulation de divorce.
 * 
 * @author Pavel BLANCO
 */
public class AnnulationDivorceAdapter extends AnnulationSeparationAdapter implements AnnulationDivorce {

	protected AnnulationDivorceAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
