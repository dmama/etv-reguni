package ch.vd.unireg.entreprise;

import org.springframework.validation.Errors;

public class DomicileViewValidator extends DateRangeViewValidator<DomicileView> {

	public DomicileViewValidator() {
		super(DomicileView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final DomicileView view = (DomicileView) target;

		if (view.getTypeAutoriteFiscale() == null) {
			errors.rejectValue("typeAutoriteFiscale", "error.tiers.type.autorite.vide");
		}
		if (view.getNoAutoriteFiscale() == null) {
			errors.rejectValue("noAutoriteFiscale", "error.tiers.autorite.vide");
		}
	}
}
