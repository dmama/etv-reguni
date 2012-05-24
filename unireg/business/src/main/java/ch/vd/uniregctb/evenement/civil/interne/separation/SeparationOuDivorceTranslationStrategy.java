package ch.vd.uniregctb.evenement.civil.interne.separation;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;

/**
 * Traitement métier des événements de séparation et divorce.
 *
 * @author Pavel BLANCO
 *
 */
public abstract class SeparationOuDivorceTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) {
		return false;
	}
}
