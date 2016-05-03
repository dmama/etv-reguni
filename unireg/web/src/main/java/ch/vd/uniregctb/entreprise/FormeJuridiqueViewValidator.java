package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;

public class FormeJuridiqueViewValidator extends DateRangeViewValidator<FormeJuridiqueView> {

	public FormeJuridiqueViewValidator() {
		super(FormeJuridiqueView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final FormeJuridiqueView view = (FormeJuridiqueView) target;

		if (view.getFormeJuridique() == null) {
			errors.rejectValue("formeJuridique", "error.tiers.forme.juridique.vide");
		}
	}
}
