package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionDateFinNationaliteAdapter extends EvenementCivilInterneBase {

	protected CorrectionDateFinNationaliteAdapter(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		if (getType() == TypeEvenementCivil.CORREC_DATE_FIN_NATIONALITE_SUISSE) {
			// TODO (PBO) CorrectionDateFinNationaliteHandler.handle
			throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
		}
		//else CORREC_DATE_FIN_NATIONALITE_NON_SUISSE rien à faire
		return null;
	}
}
