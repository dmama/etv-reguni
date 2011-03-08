package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Règles métiers permettant de traiter les événements de déménagement intra
 * communal.
 *
 * @author Ludovic Bertin
 *
 */
public class DemenagementTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new Demenagement(event, context);
	}

}
