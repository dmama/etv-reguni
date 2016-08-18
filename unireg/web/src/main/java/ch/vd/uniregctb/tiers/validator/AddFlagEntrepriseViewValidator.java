package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.tiers.view.AddFlagEntrepriseView;

public class AddFlagEntrepriseViewValidator extends AbstractFlagEntrepriseViewValidator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddFlagEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddFlagEntrepriseView view = (AddFlagEntrepriseView) target;

		// les dates
		validateRange(view, errors);

		// la valeur du flag est obligatoire
		if (view.getValue() == null) {
			errors.rejectValue("value", "error.type.flag.vide");
		}
	}
}
