package ch.vd.uniregctb.entreprise;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class AddCapitalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AddCapitalView view = (AddCapitalView) target;

			if (view.getMontant() == null) {
				errors.rejectValue("montant", "error.tiers.capital.montant.vide");
			}
			if (StringUtils.isEmpty(view.getMonnaie())) {
				errors.rejectValue("monnaie", "error.tiers.capital.monnaie.vide");
			}
		}
	}
}
