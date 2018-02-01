package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;

import ch.vd.unireg.tiers.view.EditFlagEntrepriseView;

public class EditFlagEntrepriseViewValidator extends AbstractFlagEntrepriseViewValidator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditFlagEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditFlagEntrepriseView view = (EditFlagEntrepriseView) target;

		// les dates
		validateRange(view, errors);
	}
}
