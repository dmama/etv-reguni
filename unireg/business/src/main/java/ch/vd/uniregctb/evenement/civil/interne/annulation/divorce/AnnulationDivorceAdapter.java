package ch.vd.uniregctb.evenement.civil.interne.annulation.divorce;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorceAdapter;

/**
 * Adapter pour l'annulation de divorce.
 * 
 * @author Pavel BLANCO
 */
public class AnnulationDivorceAdapter extends AnnulationSeparationOuDivorceAdapter implements AnnulationDivorce {

	protected AnnulationDivorceAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, AnnulationDivorceHandler handler) throws EvenementCivilInterneException {
		super(evenement, context, handler);
	}
}
