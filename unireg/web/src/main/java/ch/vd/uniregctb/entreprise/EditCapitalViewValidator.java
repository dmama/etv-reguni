package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditCapitalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditCapitalView view = (EditCapitalView) target;

			if (view.getMontant() == null) {
				errors.rejectValue("montant", "error.tiers.capital.montant.vide");
			}
		}
	}
}
