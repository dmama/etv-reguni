package ch.vd.unireg.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class AddEtatEntrepriseViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddEtatEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AddEtatEntrepriseView view = (AddEtatEntrepriseView) target;

			if (view.getType() == null) {
				errors.rejectValue("type", "error.tiers.etats.type.vide");
			}
			// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateObtention")) {
				if (view.getDateObtention() == null) {
					errors.rejectValue("dateObtention", "error.tiers.etats.date.vide");
				}
			}
		}
	}
}
