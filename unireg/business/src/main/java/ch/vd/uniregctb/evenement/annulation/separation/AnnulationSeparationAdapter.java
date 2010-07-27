package ch.vd.uniregctb.evenement.annulation.separation;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

/**
 * Adapter pour l'annulation de séparation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationSeparationAdapter extends GenericEvenementAdapter implements AnnulationSeparation {

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
