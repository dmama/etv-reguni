package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class AddFormeJuridiqueViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddFormeJuridiqueView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AddFormeJuridiqueView view = (AddFormeJuridiqueView) target;

			if (view.getFormeJuridique() == null) {
				errors.rejectValue("formeJuridique", "error.tiers.forme.juridique.vide");
			}

			if (view.getDateDebut() == null) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
			else {
				final RegDate today = RegDate.get();
				if (today.isBefore(view.getDateDebut())) {
					errors.rejectValue("dateDebut", "error.date.debut.future");
				}
				if (view.getDateFin() != null) {
					if (view.getDateFin().isBefore(view.getDateDebut())) {
						errors.rejectValue("dateFin", "error.date.fin.avant.debut");
					}
					else if (today.isBefore(view.getDateFin())) {
						errors.rejectValue("dateFin", "error.date.fin.future");
					}
				}
			}
		}
	}
}
