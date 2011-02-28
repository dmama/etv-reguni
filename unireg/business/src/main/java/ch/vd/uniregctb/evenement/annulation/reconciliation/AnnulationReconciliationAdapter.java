package ch.vd.uniregctb.evenement.annulation.reconciliation;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour l'annulation de réconciliation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationReconciliationAdapter extends GenericEvenementAdapter implements AnnulationReconciliation {

	protected AnnulationReconciliationAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
