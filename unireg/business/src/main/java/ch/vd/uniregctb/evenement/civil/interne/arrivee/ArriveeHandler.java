package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère l'arrivée d'un individu dans les cas suivants:
 * <ul>
 * <li>déménagement d'une commune vaudoise à l'autre (intra-cantonal)</li>
 * <li>déménagement d'un canton à l'autre (inter-cantonal)</li>
 * <li>arrivée en Suisse</li>
 * </ul>
 */
public class ArriveeHandler extends EvenementCivilHandlerBase {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE);
		types.add(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC);
		types.add(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);
		types.add(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE);
		types.add(TypeEvenementCivil.ARRIVEE_SECONDAIRE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ArriveeAdapter(event, context);
	}

}
