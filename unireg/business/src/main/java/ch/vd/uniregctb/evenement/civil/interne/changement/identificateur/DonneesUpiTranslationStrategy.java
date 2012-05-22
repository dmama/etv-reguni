package ch.vd.uniregctb.evenement.civil.interne.changement.identificateur;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class DonneesUpiTranslationStrategy implements EvenementCivilEchTranslationStrategy {
	
	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final TypeEvenementCivilEch type = event.getType();
		final ActionEvenementCivilEch action = event.getAction();
		
		if ((type == TypeEvenementCivilEch.ANNULATION_DONNEES_UPI && action != ActionEvenementCivilEch.ANNULATION) || (type != TypeEvenementCivilEch.ANNULATION_DONNEES_UPI && action == ActionEvenementCivilEch.ANNULATION)) {
			return new AnnulationIdentificateur(event, context, options);
		}
		else {
			return new ChangementIdentificateur(event, context, options);
		}
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return true;
	}
}
