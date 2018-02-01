package ch.vd.unireg.evenement.civil.interne.changement.nationalite;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Traitement métier des événements de remise à blanc de la date de fin d'une nationalité.
 *
 * @author Pavel BLANCO
 *
 */
public class RemiseBlancDateFinNationaliteTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
