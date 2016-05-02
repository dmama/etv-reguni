package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class AddDomicileViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return AddDomicileView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final AddDomicileView view = (AddDomicileView) target;

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
						errors.rejectValue("dateFin", "error.date.fin.dans.futur");
					}
				}
			}

			if (view.getTypeAutoriteFiscale() == null) {
				errors.rejectValue("typeAutoriteFiscale", "error.tiers.type.autorite.vide");
			}
			if (view.getNoAutoriteFiscale() == null) {
				errors.rejectValue("noAutoriteFiscale", "error.tiers.autorite.vide");
			}
		}
	}
}
