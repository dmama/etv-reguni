package ch.vd.unireg.evenement.civil.interne.depart;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Gère le départ d'un individu dans les cas suivants: <ul> <li>DEPART_SECONDAIRE : déménagement d'une commune vaudoise à l'autre (intra-cantonal) pour l'adresse secondaire</li> <li>DEPART_COMMUNE :
 * déménagement d'un canton à l'autre (inter-cantonal) ou Départ de Suisse</li> </ul>
 */
public class DepartRegppTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
		case DEPART_COMMUNE:
			interne = new DepartPrincipal(event, context, options);
			break;
		case DEPART_SECONDAIRE:
			interne = new DepartSecondaire(event, context, options);
			break;
		default:
			throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}
}
