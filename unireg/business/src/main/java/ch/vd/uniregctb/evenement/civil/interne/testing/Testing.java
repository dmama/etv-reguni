package ch.vd.uniregctb.evenement.civil.interne.testing;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class Testing extends EvenementCivilInterne {

	protected Testing(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
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
			erreurs.addErreur("Check completeness erreur");
			erreurs.addErreur("Again");
		}
		if (getNumeroEvenement().equals(125L)) {
			warnings.addWarning("Check completeness warn");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		return null;
	}
}
