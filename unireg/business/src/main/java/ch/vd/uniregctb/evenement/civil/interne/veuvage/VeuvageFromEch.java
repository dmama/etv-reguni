package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class VeuvageFromEch extends Veuvage {
	public VeuvageFromEch(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();

		// [UNIREG-2241] au traitement d'un événement civil de veuvage, on doit contrôler l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(getDate());
		if (etatCivil == null || etatCivil.getTypeEtatCivil() != TypeEtatCivil.VEUF) {
			erreurs.addErreur(String.format("L'individu %d n'est pas veuf dans le civil au %s", individu.getNoTechnique(), RegDateHelper.dateToDisplayString(getDate())));
		}
		else {
			final PersonnePhysique veuf = getPrincipalPP();

			/*
			 * Validations métier
			 */

			final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(veuf, getDate());
			final ValidationResults validationResults = new ValidationResults(); 
			context.getMetierService().validateForOfVeuvage(veuf,getDate(), couple, validationResults);
			addValidationResults(erreurs, warnings, validationResults);
		}
	}

}
