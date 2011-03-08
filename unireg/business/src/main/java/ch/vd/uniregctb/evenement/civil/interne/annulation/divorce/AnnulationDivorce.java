package ch.vd.uniregctb.evenement.civil.interne.annulation.divorce;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorce;

/**
 * Adapter pour l'annulation de divorce.
 * 
 * @author Pavel BLANCO
 */
public class AnnulationDivorce extends AnnulationSeparationOuDivorce {

	protected AnnulationDivorce(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}
}
