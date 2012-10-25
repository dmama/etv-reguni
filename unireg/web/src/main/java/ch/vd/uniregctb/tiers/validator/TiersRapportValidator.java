package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.rapport.view.RapportView;

public class TiersRapportValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return RapportView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		RapportView rapportView = (RapportView) obj;

		if (rapportView.getDateDebut() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDebut", "error.date.debut.vide");
		}
		if( rapportView.getTauxActivite() != null && ( rapportView.getTauxActivite() > 100 || rapportView.getTauxActivite() < 0)){
			errors.rejectValue("tauxActivite", "error.tauxActivite.invalide");
		}
	}

}
