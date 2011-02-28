package ch.vd.uniregctb.evenement.fin.nationalite;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour la fin obtention d'une nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class FinNationaliteAdapter extends GenericEvenementAdapter implements FinNationalite {

	protected FinNationaliteAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
