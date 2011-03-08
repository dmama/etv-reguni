package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Règles métiers permettant de traiter les événements d'annulation de permis C.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermisTranslationStrategy extends AnnulationPermisCOuNationaliteSuisseTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new AnnulationPermis(event, context);
	}

}
