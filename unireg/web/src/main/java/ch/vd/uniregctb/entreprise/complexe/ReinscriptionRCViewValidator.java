package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class ReinscriptionRCViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ReinscriptionRCView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ReinscriptionRCView view = (ReinscriptionRCView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateRadiationRC")) {
			if (view.getDateRadiationRC() == null) {
				errors.rejectValue("dateRadiationRC", "error.date.radiation.rc.vide");
			}
			else if (view.getDateRadiationRC().isAfter(RegDate.get())) {
				errors.rejectValue("dateRadiationRC", "error.date.radiation.rc.future");
			}
		}
	}
}
