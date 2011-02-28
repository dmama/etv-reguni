package ch.vd.uniregctb.evenement.annulation.mariage;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour l'annulation de mariage.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationMariageAdapter extends GenericEvenementAdapter implements AnnulationMariage {

	protected AnnulationMariageAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
