package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Règles métiers permettant de traiter les événements de démangement vaudois.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionNationaliteHandler extends ObtentionPermisCOuNationaliteSuisseHandler {

	@Override
	protected Set<TypeEvenementCivil>  getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ObtentionNationalite(event, context);
	}

}
