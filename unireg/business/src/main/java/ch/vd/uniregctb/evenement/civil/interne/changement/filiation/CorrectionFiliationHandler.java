package ch.vd.uniregctb.evenement.civil.interne.changement.filiation;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.AbstractChangementHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionFiliationHandler extends AbstractChangementHandler {

	@Override
	public EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new CorrectionFiliation(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		final Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_FILIATION);
		return types;
	}

}
