package ch.vd.uniregctb.evenement.annulation.separation;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour l'annulation de séparation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationSeparationAdapter extends AnnulationSeparationOuDivorceAdapter implements AnnulationSeparation {

	protected AnnulationSeparationAdapter(EvenementCivilData evenement, EvenementCivilContext context, AnnulationSeparationHandler handler) throws EvenementAdapterException {
		super(evenement, context, handler);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
