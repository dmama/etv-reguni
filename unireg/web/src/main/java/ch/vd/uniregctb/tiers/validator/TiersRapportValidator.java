package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.rapport.view.RapportView;

public class TiersRapportValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		RapportView rapportView = (RapportView) obj;

		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (rapportView.getDateDebut() == null) {
				ValidationUtils.rejectIfEmpty(errors, "dateDebut", "error.date.debut.vide");
			}
		}
	}

}
