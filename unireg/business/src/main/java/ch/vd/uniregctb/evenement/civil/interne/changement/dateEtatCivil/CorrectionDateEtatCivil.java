package ch.vd.uniregctb.evenement.civil.interne.changement.dateEtatCivil;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class CorrectionDateEtatCivil extends EvenementCivilInterne {

	protected CorrectionDateEtatCivil(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// TODO (PBO) CorrectionDateEtatCivilHandler.validateSpecific
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// TODO (PBO) CorrectionDateEtatCivilHandler.handle
		throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
	}
}
