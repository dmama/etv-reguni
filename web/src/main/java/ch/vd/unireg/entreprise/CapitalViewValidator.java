package ch.vd.unireg.entreprise;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

public class CapitalViewValidator extends DateRangeViewValidator<CapitalView> {

	public CapitalViewValidator() {
		super(CapitalView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final CapitalView view = (CapitalView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("montant")) {
			if (view.getMontant() == null) {
				errors.rejectValue("montant", "error.tiers.capital.montant.vide");
			}
		}
		if (StringUtils.isEmpty(view.getMonnaie())) {
			errors.rejectValue("monnaie", "error.tiers.capital.monnaie.vide");
		}
	}
}
