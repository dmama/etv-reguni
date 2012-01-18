package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Gère l'arrivée d'un individu dans les cas suivants:
 * <ul>
 * <li>déménagement d'une commune vaudoise à l'autre (intra-cantonal)</li>
 * <li>déménagement d'un canton à l'autre (inter-cantonal)</li>
 * <li>arrivée en Suisse</li>
 * </ul>
 */
public class ArriveeTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		// arrivée principale ou secondaire ?
		if (isArriveePrincipale(event)) {
			return new ArriveePrincipale(event, context, options);
		}
		else {
			return new ArriveeSecondaire(event, context, options);
		}
	}

	private boolean isArriveePrincipale(EvenementCivilExterne event) {
		switch (event.getType()) {
			case ARRIVEE_DANS_COMMUNE:
			case ARRIVEE_PRINCIPALE_HC:
			case ARRIVEE_PRINCIPALE_HS:
			case ARRIVEE_PRINCIPALE_VAUDOISE:
			case DEMENAGEMENT_DANS_COMMUNE:
				return true;

			case ARRIVEE_SECONDAIRE:
				return false;

			default:
				throw new IllegalArgumentException("Type d'arrivée non supporté : " + event.getType());
		}
	}
}
