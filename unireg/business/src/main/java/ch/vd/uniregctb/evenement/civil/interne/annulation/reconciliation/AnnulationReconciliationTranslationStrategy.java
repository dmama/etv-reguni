package ch.vd.uniregctb.evenement.civil.interne.annulation.reconciliation;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Traitements métier pour événements d'annulation de réconciliation.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationReconciliationTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationReconciliation(event, context, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationReconciliation(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
		return false;
	}
}
