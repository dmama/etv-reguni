package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère la fin d'obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public class FinPermisHandler extends EvenementCivilHandlerBase {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER);
		return types;
	}

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new FinPermis(event, context);
	}

}
