package ch.vd.uniregctb.rt.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.rt.view.RapportPrestationView;

public class RapportPrestationEditValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportPrestationView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {

		RapportPrestationView rapportView = (RapportPrestationView) obj;

		if (rapportView.getDateDebut() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDebut", "error.date.debut.vide");
		}
	}

}