package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour la suppresion de l'obtention d'une nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteAdapter extends GenericEvenementAdapter implements SuppressionNationalite {

	protected SuppressionNationaliteAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
