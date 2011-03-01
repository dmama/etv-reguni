package ch.vd.uniregctb.evenement.annulation.divorce;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.annulation.separation.AnnulationSeparationOuDivorceAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour l'annulation de divorce.
 * 
 * @author Pavel BLANCO
 */
public class AnnulationDivorceAdapter extends AnnulationSeparationOuDivorceAdapter implements AnnulationDivorce {

	protected AnnulationDivorceAdapter(EvenementCivilData evenement, EvenementCivilContext context, AnnulationDivorceHandler handler) throws EvenementAdapterException {
		super(evenement, context, handler);
	}
}
