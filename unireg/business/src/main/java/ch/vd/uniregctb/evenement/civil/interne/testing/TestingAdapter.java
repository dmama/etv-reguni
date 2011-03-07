package ch.vd.uniregctb.evenement.civil.interne.testing;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class TestingAdapter extends EvenementCivilInterneBase {

	protected TestingAdapter(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		if (getNumeroEvenement().equals(121L)) {
			// On ne fait rien
		}
		if (getNumeroEvenement().equals(122L)) {
			// a faire
		}
		if (getNumeroEvenement().equals(123L)) {
			// On throw une Exception
			throw new RuntimeException("L'événement n'est pas complet");
		}
		if (getNumeroEvenement().equals(124L)) {
			erreurs.add(new EvenementCivilExterneErreur("Check completeness erreur"));
			erreurs.add(new EvenementCivilExterneErreur("Again"));
		}
		if (getNumeroEvenement().equals(125L)) {
			warnings.add(new EvenementCivilExterneErreur("Check completeness warn"));
		}
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return null;
	}
}
