package ch.vd.uniregctb.evenement.changement.dateEtatCivil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionDateEtatCivilHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}
	
	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// TODO (PBO) CorrectionDateEtatCivilHandler.validateSpecific
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// TODO (PBO) CorrectionDateEtatCivilHandler.handle
		throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new CorrectionDateEtatCivilAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_DATE_ETAT_CIVIL);
		return types;
	}

}
