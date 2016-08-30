package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class AnnulationDemenagementSiegeViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AnnulationDemenagementSiegeView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AnnulationDemenagementSiegeView view = (AnnulationDemenagementSiegeView) target;

		if (view.getDateDebutSiegeActuel() == null) {
			errors.reject("error.date.debut.vide");
		}
	}
}
