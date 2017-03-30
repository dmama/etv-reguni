package ch.vd.uniregctb.registrefoncier;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class EditDemandeDegrevementViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditDemandeDegrevementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditDemandeDegrevementView view = (EditDemandeDegrevementView) target;

		if (view.getPeriodeFiscale() == null && !errors.hasFieldErrors("periodeFiscale")) {
			errors.rejectValue("periodeFiscale", "error.champ.obligatoire");
		}
		if (view.getDelaiRetour() == null && !errors.hasFieldErrors("delaiRetour")) {
			errors.rejectValue("delaiRetour", "error.champ.obligatoire");
		}
		if (view.getDateRetour() == null && !errors.hasFieldErrors("dateRetour")) {
			errors.rejectValue("dateRetour", "error.champ.obligatoire");
		}
		if (view.getDateRetour() != null && view.getDateRetour().isAfter(RegDate.get())) {
			errors.rejectValue("dateRetour", "error.date.retour.future");
		}
	}
}
