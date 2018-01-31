package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.tiers.view.AddRegimeFiscalView;

public class AddRegimeFiscalViewValidator extends AbstractRegimeFiscalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddRegimeFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddRegimeFiscalView view = (AddRegimeFiscalView) target;
		doValidate(view, errors);
	}
}
