package ch.vd.uniregctb.evenement.changement.dateEtatCivil;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class CorrectionDateEtatCivilAdapter extends GenericEvenementAdapter implements CorrectionDateEtatCivil {

	protected CorrectionDateEtatCivilAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
	}
}
