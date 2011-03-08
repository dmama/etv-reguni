package ch.vd.uniregctb.evenement.civil.interne.fin.nationalite;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement métier des événements fin d'une nationalité.
 *
 * @author Pavel BLANCO
 *
 */
public class FinNationaliteHandler extends EvenementCivilHandlerBase {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.FIN_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.FIN_NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new FinNationalite(event, context);
	}
}
