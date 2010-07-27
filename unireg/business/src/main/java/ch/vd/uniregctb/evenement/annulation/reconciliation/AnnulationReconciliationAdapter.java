package ch.vd.uniregctb.evenement.annulation.reconciliation;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

/**
 * Adapter pour l'annulation de réconciliation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationReconciliationAdapter extends GenericEvenementAdapter implements AnnulationReconciliation {

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
