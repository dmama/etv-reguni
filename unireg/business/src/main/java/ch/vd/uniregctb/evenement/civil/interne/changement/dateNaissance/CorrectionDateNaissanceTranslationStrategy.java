package ch.vd.uniregctb.evenement.civil.interne.changement.dateNaissance;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.changement.AbstractChangementTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.correction.identification.CorrectionIdentificationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

public class CorrectionDateNaissanceTranslationStrategy extends AbstractChangementTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new CorrectionDateNaissance(event, context, options);
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {
		throw new EvenementCivilException(
				String.format (
						"La correction de date de naissance passe par une correction d'identification pour un evenement ech cf. la classe %s"
						,CorrectionIdentificationTranslationStrategy.class.getSimpleName()
				)
		);

	}

}
