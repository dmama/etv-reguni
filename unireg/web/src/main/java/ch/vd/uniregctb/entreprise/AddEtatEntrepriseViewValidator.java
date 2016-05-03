package ch.vd.uniregctb.entreprise;

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
			if (view.getDateObtention() == null) {
				errors.rejectValue("dateObtention", "error.tiers.etats.date.vide");
			}
		}
	}
}
