package ch.vd.uniregctb.evenement.veuvage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement métier pour événements de veuvage.
 *
 * @author Pavel BLANCO
 *
 */
public class VeuvageHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		Veuvage veuvage = (Veuvage) target;

		long numeroIndividu = veuvage.getIndividu().getNoTechnique();
		PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu(numeroIndividu);

		/*
		 * Validations métier
		 */
		ValidationResults validationResults = getMetier().validateVeuvage(veuf, veuvage.getDate());
		addValidationResults(erreurs, warnings, validationResults);
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		/*
		 * Cas de veuvage
		 */
		Veuvage veuvage = (Veuvage) evenement;

		/*
		 * Obtention du tiers correspondant au veuf.
		 */
		PersonnePhysique veuf = getService().getPersonnePhysiqueByNumeroIndividu(veuvage.getIndividu().getNoTechnique());

		/*
		 * Traitement de l'événement
		 */
		getMetier().veuvage(veuf, veuvage.getDateVeuvage(), null, veuvage.getNumeroEvenement());
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new VeuvageAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.VEUVAGE);
		return types;
	}

}
