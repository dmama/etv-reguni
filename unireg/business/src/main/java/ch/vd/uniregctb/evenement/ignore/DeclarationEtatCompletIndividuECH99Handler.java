package ch.vd.uniregctb.evenement.ignore;

import ch.vd.uniregctb.evenement.common.EvenementCivilIgnoreHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import java.util.HashSet;
import java.util.Set;

/**
 * handler spécifique à l'événement civil ECH99
 */
public class DeclarationEtatCompletIndividuECH99Handler extends EvenementCivilIgnoreHandler {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		final Set<TypeEvenementCivil> set = new HashSet<TypeEvenementCivil>(1);
		set.add(TypeEvenementCivil.ETAT_COMPLET);
		return set;
	}

}
