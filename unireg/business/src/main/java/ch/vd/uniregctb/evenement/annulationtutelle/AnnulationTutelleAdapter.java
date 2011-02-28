package ch.vd.uniregctb.evenement.annulationtutelle;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter pour l'annulation de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationTutelleAdapter extends GenericEvenementAdapter implements AnnulationTutelle {

	protected AnnulationTutelleAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
