package ch.vd.unireg.identification.contribuable.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.identification.contribuable.view.NonIdentificationMessageEditView;

public class NonIdentificationMessageEditValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return NonIdentificationMessageEditView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final NonIdentificationMessageEditView view = (NonIdentificationMessageEditView) target;
		if (view.getErreurMessage() == null) {
			errors.rejectValue("erreurMessage", "error.champ.obligatoire");
		}
	}
}
