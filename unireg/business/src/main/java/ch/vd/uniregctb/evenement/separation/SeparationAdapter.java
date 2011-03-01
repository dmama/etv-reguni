package ch.vd.uniregctb.evenement.separation;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class SeparationAdapter extends SeparationOuDivorceAdapter implements Separation {

	protected SeparationAdapter(EvenementCivilData evenement, EvenementCivilContext context, SeparationHandler handler) throws EvenementAdapterException {
		super(evenement, context, handler);
	}
}
