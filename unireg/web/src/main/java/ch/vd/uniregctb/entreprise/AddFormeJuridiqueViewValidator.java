package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class AddFormeJuridiqueViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AddFormeJuridiqueView view = (AddFormeJuridiqueView) target;

			if (view.getFormeJuridique() == null) {
				errors.rejectValue("formeJuridique", "error.tiers.forme.juridique.vide");
			}
		}
	}
}
