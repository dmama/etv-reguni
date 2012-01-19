package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Traitement métier des événements de remise à blanc de la date de fin d'une nationalité.
 *
 * @author Pavel BLANCO
 *
 */
public class RemiseBlancDateFinNationaliteTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case ANNUL_DATE_FIN_NATIONALITE_SUISSE:
				interne = new RemiseBlancDateFinNationaliteSuisse(event, context, options);
				break;
			case ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE:
				interne = new RemiseBlancDateFinNationaliteNonSuisse(event, context, options);
				break;
			default:
				throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}
}
