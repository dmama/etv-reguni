package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère le départ d'un individu dans les cas suivants:
 * <ul>
 * <li>DEPART_SECONDAIRE : déménagement d'une commune vaudoise à l'autre (intra-cantonal) pour l'adresse secondaire</li>
 * <li>DEPART_COMMUNE : déménagement d'un canton à l'autre (inter-cantonal) ou Départ de Suisse</li>
 * </ul>
 */
public class DepartHandler extends EvenementCivilHandlerBase {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.DEPART_COMMUNE);
		types.add(TypeEvenementCivil.DEPART_SECONDAIRE);
		return types;
	}

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new Depart(event, context);
	}
}
