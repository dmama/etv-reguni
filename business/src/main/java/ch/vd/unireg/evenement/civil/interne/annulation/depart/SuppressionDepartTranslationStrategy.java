package ch.vd.unireg.evenement.civil.interne.annulation.depart;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Gère l'annulation de départ d'un individu dans les cas suivants:
 * <ul>
 * <li>SUPPRESSION_DEPART_COMMUNE : départ de la commune</li>
 * <li>SUPPRESSION_DEPART_SECONDAIRE : départ secondaire</li>
 * </ul>
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionDepartTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new SuppressionDepart(event, context, options);
	}
}
