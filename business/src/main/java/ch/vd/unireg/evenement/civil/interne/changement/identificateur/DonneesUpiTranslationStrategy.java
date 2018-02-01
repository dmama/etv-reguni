package ch.vd.unireg.evenement.civil.interne.changement.identificateur;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class DonneesUpiTranslationStrategy implements EvenementCivilEchTranslationStrategy {
	
	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return true;
	}
}
