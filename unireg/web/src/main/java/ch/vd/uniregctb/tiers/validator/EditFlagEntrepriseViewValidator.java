package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.tiers.view.EditFlagEntrepriseView;

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
