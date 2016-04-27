package ch.vd.uniregctb.entreprise;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class AddRaisonSocialeViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddRaisonSocialeView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AddRaisonSocialeView view = (AddRaisonSocialeView) target;

			if (StringUtils.isBlank(view.getRaisonSociale())) {
				errors.rejectValue("raisonSociale", "error.tiers.raison.sociale.vide");
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
