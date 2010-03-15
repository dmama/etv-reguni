package ch.vd.uniregctb.rt.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.rt.view.RapportPrestationView;

public class RapportPrestationEditValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportPrestationView.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {

		RapportPrestationView rapportView = (RapportPrestationView) obj;

		if (rapportView.getDateDebut() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDebut", "error.date.debut.vide");
		}
		if( rapportView.getTauxActivite() != null && ( rapportView.getTauxActivite() > 100 || rapportView.getTauxActivite() < 0)){
			errors.rejectValue("tauxActivite", "error.tauxActivite.invalide");
		}
	}

}