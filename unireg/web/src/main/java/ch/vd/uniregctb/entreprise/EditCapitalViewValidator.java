package ch.vd.uniregctb.entreprise;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class EditCapitalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditCapitalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditCapitalView view = (EditCapitalView) target;

			if (view.getMontant() == null) {
				errors.rejectValue("montant", "error.tiers.capital.montant.vide");
			}

			if (StringUtils.isEmpty(view.getMonnaie())) {
				errors.rejectValue("monnaie", "error.tiers.capital.monnaie.vide");
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
