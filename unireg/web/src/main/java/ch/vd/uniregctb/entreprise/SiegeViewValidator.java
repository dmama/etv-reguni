package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;

public class SiegeViewValidator extends DateRangeViewValidator<SiegeView> {

	public SiegeViewValidator() {
		super(SiegeView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final SiegeView view = (SiegeView) target;

		if (view.getTypeAutoriteFiscale() == null) {
			errors.rejectValue("typeAutoriteFiscale", "error.tiers.type.autorite.vide");
		}
		if (view.getNoAutoriteFiscale() == null) {
			errors.rejectValue("noAutoriteFiscale", "error.tiers.autorite.vide");
		}
	}
}
