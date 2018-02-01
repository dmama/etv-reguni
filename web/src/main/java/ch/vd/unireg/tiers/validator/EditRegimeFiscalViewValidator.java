package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.tiers.view.EditRegimeFiscalView;

public class EditRegimeFiscalViewValidator extends AbstractRegimeFiscalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditRegimeFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditRegimeFiscalView view = (EditRegimeFiscalView) target;
		doValidate(view, errors);
	}
}
