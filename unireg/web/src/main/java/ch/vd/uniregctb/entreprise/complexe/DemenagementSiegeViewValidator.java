package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class DemenagementSiegeViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return DemenagementSiegeView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final DemenagementSiegeView view = (DemenagementSiegeView) target;

		if (view.getDateDebutNouveauSiege() == null) {
			errors.rejectValue("dateDebutNouveauSiege", "error.date.debut.vide");
		}
		else if (view.getDateDebutNouveauSiege().isAfter(RegDate.get())) {
			errors.rejectValue("dateDebutNouveauSiege", "error.date.debut.future");
		}

		if (view.getNoAutoriteFiscale() == null) {
			errors.rejectValue("noAutoriteFiscale", "error.autorite.fiscale.vide");
		}
		if (view.getTypeAutoriteFiscale() == null) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.fiscale.vide");
		}
	}
}
