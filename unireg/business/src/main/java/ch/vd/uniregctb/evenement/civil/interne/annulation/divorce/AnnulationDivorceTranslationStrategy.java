package ch.vd.uniregctb.evenement.civil.interne.annulation.divorce;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorceTranslationStrategy;

/**
 * Traitements métier pour événements d'annulation de divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationDivorceTranslationStrategy extends AnnulationSeparationOuDivorceTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new AnnulationDivorce(event, context);
	}

}
