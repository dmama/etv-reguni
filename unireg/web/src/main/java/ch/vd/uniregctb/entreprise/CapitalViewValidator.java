package ch.vd.uniregctb.entreprise;

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

		if (view.getMontant() == null) {
			errors.rejectValue("montant", "error.tiers.capital.montant.vide");
		}
		if (StringUtils.isEmpty(view.getMonnaie())) {
			errors.rejectValue("monnaie", "error.tiers.capital.monnaie.vide");
		}
	}
}
