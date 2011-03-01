package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class EvenementCivilIgnoreAdapter extends GenericEvenementAdapter {

	protected EvenementCivilIgnoreAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	public boolean isContribuablePresentBefore() {
		return false;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// pas de validation
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// pas de validation
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// simplement ignoré
		return null;
	}
}
