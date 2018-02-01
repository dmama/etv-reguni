package ch.vd.unireg.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class ScissionEntrepriseViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ScissionEntrepriseView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ScissionEntrepriseView view = (ScissionEntrepriseView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateContratScission")) {
			if (view.getDateContratScission() == null) {
				errors.rejectValue("dateContratScission", "error.date.contrat.scission.vide");
			}
			else if (view.getDateContratScission().isAfter(RegDate.get())) {
				errors.rejectValue("dateContratScission", "error.date.contrat.scission.future");
			}
		}
	}
}
