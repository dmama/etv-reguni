package ch.vd.uniregctb.entreprise.complexe;

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

		if (view.getDateContratScission() == null) {
			errors.rejectValue("dateContratScission", "error.date.contrat.scission.vide");
		}
		else if (view.getDateContratScission().isAfter(RegDate.get())) {
			errors.rejectValue("dateContratScission", "error.date.contrat.scission.future");
		}
	}
}
