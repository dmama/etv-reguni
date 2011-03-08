package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class ChangementNationalite extends EvenementCivilInterne {

	protected ChangementNationalite(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		throw new NotImplementedException();
	}
}
