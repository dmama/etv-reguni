package ch.vd.uniregctb.evenement.civil.interne.mariage;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Règles métiers permettant de traiter les événements mariage ou de partenariat enregistré.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class MariageTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new Mariage(event, context);
	}

}
