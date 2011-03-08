package ch.vd.uniregctb.evenement.civil.interne.divorce;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationOuDivorceHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement métier des événements divorce.
 * 
 * @author Pavel BLANCO
 *
 */
public class DivorceHandler extends SeparationOuDivorceHandler {

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.DIVORCE);
		return types;
	}

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new Divorce(event, context, this);
	}
}
