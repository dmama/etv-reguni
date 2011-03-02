package ch.vd.uniregctb.evenement.civil.interne.ignore;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilIgnoreHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

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
