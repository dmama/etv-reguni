package ch.vd.uniregctb.evenement.civil.interne.annulation.divorce;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Traitements métier pour événements d'annulation de divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationDivorceTranslationStrategy extends AnnulationSeparationOuDivorceTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationDivorce(event, context, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationDivorce(event, context, options);
	}
}
