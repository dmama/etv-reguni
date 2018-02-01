package ch.vd.unireg.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditEnseigneEtablissementViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditEnseigneEtablissementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditEnseigneEtablissementView view = (EditEnseigneEtablissementView) target;

			if (view.getTiersId() == null) {
				errors.rejectValue("tiersId", "error.tiers.inexistant");
			}
		}
	}
}
