package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditFormeJuridiqueViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditFormeJuridiqueView view = (EditFormeJuridiqueView) target;

			if (view.getFormeJuridique() == null) {
				errors.rejectValue("formeJuridique", "error.tiers.forme.juridique.vide");
			}
		}
	}
}
