package ch.vd.uniregctb.evenement.civil.interne.fin.nationalite;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.engine.externe.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Traitement métier des événements fin d'une nationalité.
 *
 * @author Pavel BLANCO
 *
 */
public class FinNationaliteTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case FIN_NATIONALITE_SUISSE:
				interne = new FinNationaliteSuisse(event, context, options);
				break;
			case FIN_NATIONALITE_NON_SUISSE:
				interne = new FinNationaliteNonSuisse(event, context, options);
				break;
			default:
				throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}
}
