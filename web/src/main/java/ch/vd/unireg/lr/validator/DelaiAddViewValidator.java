package ch.vd.unireg.lr.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.lr.view.DelaiAddView;

public class DelaiAddViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return DelaiAddView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final DelaiAddView view = (DelaiAddView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("delaiAccorde")) {
			if (view.getDelaiAccorde() == null) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.vide");
			}
			else if (view.getDelaiAccorde().isBefore(RegDate.get())) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.anterieure.date.jour");
			}
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDemande")) {
			if (view.getDateDemande() == null) {
				errors.rejectValue("dateDemande", "error.date.demande.vide");
			}
			else if (view.getDateDemande().isAfter(RegDate.get())) {
				errors.rejectValue("dateDemande", "error.date.demande.future");
			}
		}
	}
}
