package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

public class EditForPrincipalValidator extends EditForRevenuFortuneValidator {
	@Override
	public boolean supports(Class<?> clazz) {
		return EditForPrincipalView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);
		// rien de plus à faire
	}
}
