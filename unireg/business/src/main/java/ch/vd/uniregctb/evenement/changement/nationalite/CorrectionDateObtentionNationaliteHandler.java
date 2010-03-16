package ch.vd.uniregctb.evenement.changement.nationalite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionDateObtentionNationaliteHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		if (evenement.getType().equals(TypeEvenementCivil.CORREC_DATE_OBTENTION_NATIONALITE_SUISSE)) {
			// TODO (PBO) CorrectionDateObtentionNationaliteHandler.handle
			throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
		}
		//else CORREC_DATE_OBTENTION_NATIONALITE_NON_SUISSE rien à faire
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_DATE_OBTENTION_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.CORREC_DATE_OBTENTION_NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new CorrectionDateObtentionNationaliteAdapter();
	}

}
