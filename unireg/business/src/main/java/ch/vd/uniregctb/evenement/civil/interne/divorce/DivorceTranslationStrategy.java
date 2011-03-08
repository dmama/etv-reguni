package ch.vd.uniregctb.evenement.civil.interne.divorce;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationOuDivorceTranslationStrategy;

/**
 * Traitement métier des événements divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class DivorceTranslationStrategy extends SeparationOuDivorceTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new Divorce(event, context, this);
	}
}
