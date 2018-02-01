package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;

import ch.vd.unireg.tiers.view.EditAllegementFiscalView;

public class EditAllegementFiscalViewValidator extends AbstractAllegementFiscalViewValidator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditAllegementFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditAllegementFiscalView view = (EditAllegementFiscalView) target;

		// les dates
		validateRange(view, errors);
	}
}
