package ch.vd.unireg.evenement.civil.interne.changement.sexe;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.changement.AbstractChangementTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.correction.identification.CorrectionIdentificationTranslationStrategy;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

public class ChangementSexeTranslationStrategy extends AbstractChangementTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new ChangementSexe(event, context, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		throw new EvenementCivilException(
				String.format (
						"Le changement de sexe passe par une correction d'identification pour un evenement ech cf. la classe %s"
						,CorrectionIdentificationTranslationStrategy.class.getSimpleName()
				)
		);
	}
}
