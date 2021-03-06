package ch.vd.unireg.registrefoncier.communaute;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class AddPrincipalViewValidator implements Validator {
	@Override
	public boolean supports(Class<?> clazz) {
		return AddPrincipalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddPrincipalView view = (AddPrincipalView) target;
		if (view.getPeriodeDebut() == null) {
			errors.rejectValue("periodeDebut", "error.periode.fiscale.vide");
		}
		else {
			final int periode = view.getPeriodeDebut();
			if (periode < 1900 || periode > 9999) {
				errors.rejectValue("periodeDebut", "error.param.annee");
			} else if (periode > RegDate.get().addYears(1).year()) {
				errors.rejectValue("periodeDebut", "error.principal.periode.fiscale");
			}
		}
	}
}
