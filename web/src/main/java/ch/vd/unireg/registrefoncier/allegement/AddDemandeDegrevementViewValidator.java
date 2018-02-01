package ch.vd.unireg.registrefoncier.allegement;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class AddDemandeDegrevementViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddDemandeDegrevementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddDemandeDegrevementView view = (AddDemandeDegrevementView) target;

		if (view.getPeriodeFiscale() == null && !errors.hasFieldErrors("periodeFiscale")) {
			errors.rejectValue("periodeFiscale", "error.champ.obligatoire");
		}
		if (view.getDelaiRetour() == null && !errors.hasFieldErrors("delaiRetour")) {
			errors.rejectValue("delaiRetour", "error.champ.obligatoire");
		}
		else if (view.getDelaiRetour() != null && view.getDelaiRetour().isBefore(RegDate.get())) {
			errors.rejectValue("delaiRetour", "error.delai.accorde.anterieure.date.jour");
		}
	}
}
