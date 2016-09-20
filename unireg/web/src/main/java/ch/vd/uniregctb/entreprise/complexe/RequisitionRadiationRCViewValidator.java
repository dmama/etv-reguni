package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.RegDate;

public class RequisitionRadiationRCViewValidator extends FinActiviteViewValidator {

	@Override
	public boolean supports(Class<?> clazz) {
		return RequisitionRadiationRCView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final RequisitionRadiationRCView view = (RequisitionRadiationRCView) target;

		if (view.isImprimerDemandeBilanFinal()) {
			if (!errors.hasFieldErrors("periodeFiscale")) {
				if (view.getPeriodeFiscale() == null) {
					errors.rejectValue("periodeFiscale", "error.periode.fiscale.vide");
				}
				else if (view.getPeriodeFiscale() > RegDate.get().year()) {
					errors.rejectValue("periodeFiscale", "error.periode.fiscale.future");
				}
			}
		}
	}
}
