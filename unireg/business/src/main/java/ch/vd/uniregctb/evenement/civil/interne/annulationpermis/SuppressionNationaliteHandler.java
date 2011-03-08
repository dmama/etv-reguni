package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements de suppression de nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteHandler extends AnnulationPermisCOuNationaliteSuisseHandler {

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new SuppressionNationalite(event, context);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.SUP_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE);
		return types;
	}
}
