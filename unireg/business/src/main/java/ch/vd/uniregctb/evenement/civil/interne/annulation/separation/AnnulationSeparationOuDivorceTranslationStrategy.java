package ch.vd.uniregctb.evenement.civil.interne.annulation.separation;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;

public abstract class AnnulationSeparationOuDivorceTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}
}