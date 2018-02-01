package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class FailliteViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return FailliteView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final FailliteView view = (FailliteView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("datePrononceFaillite")) {
			if (view.getDatePrononceFaillite() == null) {
				errors.rejectValue("datePrononceFaillite", "error.date.prononce.faillite.vide");
			}
			else if (view.getDatePrononceFaillite().isAfter(RegDate.get())) {
				errors.rejectValue("datePrononceFaillite", "error.date.prononce.faillite.future");
			}
		}
	}
}
