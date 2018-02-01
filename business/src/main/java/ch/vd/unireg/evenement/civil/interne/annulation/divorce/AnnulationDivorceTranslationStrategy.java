package ch.vd.unireg.evenement.civil.interne.annulation.divorce;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorceTranslationStrategy;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

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
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationDivorce(event, context, options);
	}
}
