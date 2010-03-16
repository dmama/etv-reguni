package ch.vd.uniregctb.evenement.changement.nationalite;

import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ChangementNationaliteHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		return null;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ChangementNationaliteAdapter();
	}

}
