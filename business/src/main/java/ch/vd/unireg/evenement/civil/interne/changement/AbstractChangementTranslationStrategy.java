package ch.vd.unireg.evenement.civil.interne.changement;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;

/**
 * Handler avec comportement par défaut la réindexation de l'individu à l'origine de l'événement.
 *
 * @author Pavel BLANCO
 *
 */
public abstract class AbstractChangementTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) {
		return true;
	}
}
