package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Règles métiers permettant de traiter les événements de démangement vaudois.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionNationaliteTranslationStrategy extends ObtentionPermisCOuNationaliteSuisseTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new ObtentionNationalite(event, context);
	}

}
