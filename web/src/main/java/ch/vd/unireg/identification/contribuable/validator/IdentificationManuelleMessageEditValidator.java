package ch.vd.unireg.identification.contribuable.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.identification.contribuable.view.IdentificationManuelleMessageEditView;

public class IdentificationManuelleMessageEditValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return IdentificationManuelleMessageEditView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final IdentificationManuelleMessageEditView view = (IdentificationManuelleMessageEditView) target;
		if (view.getContribuableIdentifie() == null) {
			errors.rejectValue("contribuableIdentifie", "error.champ.obligatoire");
		}
	}
}
