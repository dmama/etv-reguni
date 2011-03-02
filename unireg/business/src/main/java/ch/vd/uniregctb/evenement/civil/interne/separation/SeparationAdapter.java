package ch.vd.uniregctb.evenement.civil.interne.separation;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class SeparationAdapter extends SeparationOuDivorceAdapter implements Separation {

	protected SeparationAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, SeparationHandler handler) throws EvenementCivilInterneException {
		super(evenement, context, handler);
	}
}
