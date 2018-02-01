package ch.vd.unireg.evenement.civil.interne.divorce;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.separation.SeparationOuDivorceTranslationStrategy;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Traitement métier des événements divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class DivorceTranslationStrategy extends SeparationOuDivorceTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new Divorce(event, context, this, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new Divorce(event, context, options);
	}
}
