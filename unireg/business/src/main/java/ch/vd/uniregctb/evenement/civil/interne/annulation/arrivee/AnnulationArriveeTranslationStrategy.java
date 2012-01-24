package ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Gère les événements suivants:
 * <ul>
 * <li>suppression arrivée dans la commune</li>
 * <li>annulation arrivée secondaire</li>
 * </ul>
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationArriveeTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationArrivee(event, context, options);
	}
}
