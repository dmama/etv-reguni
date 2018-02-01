package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.rapport.view.RapportView;

public class RapportEditValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		final RapportView view = (RapportView) obj;

		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (view.getDateDebut() == null) {
				ValidationUtils.rejectIfEmpty(errors, "dateDebut", "error.date.debut.vide");
			}
		}

		if (view.getDateFin() != null && view.getDateDebut() != null && view.getDateFin().isBefore(view.getDateDebut())) {
			errors.rejectValue("dateFin", "rapport.interval.dateFin", "la date ne peut être antérieur à la date de début.");
		}
	}

}
