package ch.vd.unireg.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class FinActiviteViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return FinActiviteView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final FinActiviteView view = (FinActiviteView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFinActivite")) {
			if (view.getDateFinActivite() == null) {
				errors.rejectValue("dateFinActivite", "error.date.fin.vide");
			}
			else if (view.getDateFinActivite().isAfter(RegDate.get())) {
				errors.rejectValue("dateFinActivite", "error.date.fin.dans.futur");
			}
		}
	}
}
