package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Règles métiers permettant de traiter les événements de suppression de nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteTranslationStrategy extends AnnulationPermisCOuNationaliteSuisseTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case SUP_NATIONALITE_SUISSE:
				interne = new SuppressionNationaliteSuisse(event, context, options);
				break;
			case SUP_NATIONALITE_NON_SUISSE:
				interne = new SuppressionNationaliteNonSuisse(event, context, options);
				break;
		    default:
			    throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}

}
