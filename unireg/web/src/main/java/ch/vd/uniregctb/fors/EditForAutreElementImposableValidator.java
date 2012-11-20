package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

public class EditForAutreElementImposableValidator extends EditForRevenuFortuneValidator {
	@Override
	public boolean supports(Class<?> clazz) {
		return EditForAutreElementImposableView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);
		// rien de plus à faire
	}
}
