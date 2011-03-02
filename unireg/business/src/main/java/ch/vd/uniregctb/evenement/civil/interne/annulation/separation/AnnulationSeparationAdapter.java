package ch.vd.uniregctb.evenement.civil.interne.annulation.separation;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;

/**
 * Adapter pour l'annulation de séparation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationSeparationAdapter extends AnnulationSeparationOuDivorceAdapter implements AnnulationSeparation {

	protected AnnulationSeparationAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, AnnulationSeparationHandler handler) throws EvenementCivilInterneException {
		super(evenement, context, handler);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
