package ch.vd.uniregctb.evenement.testing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class TestingHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		if (target.getNumeroEvenement().equals(121L)) {
			// On ne fait rien
		}
		if (target.getNumeroEvenement().equals(122L)) {
			// a faire
		}
		if (target.getNumeroEvenement().equals(123L)) {
			// On throw une Exception
			throw new RuntimeException("L'événement n'est pas complet");
		}
		if (target.getNumeroEvenement().equals(124L)) {
			erreurs.add(new EvenementCivilErreur("Check completeness erreur"));
			erreurs.add(new EvenementCivilErreur("Again"));
		}
		if (target.getNumeroEvenement().equals(125L)) {
			warnings.add(new EvenementCivilErreur("Check completeness warn"));
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		return null;
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	public GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException {
		return new TestingAdapter(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.EVENEMENT_TESTING);
		return types;
	}

}
