package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.view.CloseActiviteEconomiqueView;

public class CloseActiviteEconomiqueValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return CloseActiviteEconomiqueView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final CloseActiviteEconomiqueView view = (CloseActiviteEconomiqueView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFin")) {
			if (view.getDateFin() == null) {
				errors.rejectValue("dateFin", "error.date.fin.vide");
			}
			else if (view.getDateDebut() != null && view.getDateFin().isBefore(view.getDateDebut())) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
			else if (RegDate.get().isBefore(view.getDateFin())) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
		}
	}
}
