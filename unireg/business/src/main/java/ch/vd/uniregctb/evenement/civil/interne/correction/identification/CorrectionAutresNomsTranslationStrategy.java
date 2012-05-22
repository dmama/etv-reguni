package ch.vd.uniregctb.evenement.civil.interne.correction.identification;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.changement.nom.ChangementNom;

public class CorrectionAutresNomsTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new ChangementNom(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return true;
	}
}
