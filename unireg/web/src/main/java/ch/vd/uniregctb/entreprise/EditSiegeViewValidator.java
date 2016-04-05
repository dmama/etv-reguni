package ch.vd.uniregctb.entreprise;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class EditSiegeViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EtablissementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final EditSiegeView view = (EditSiegeView) target;

			if (view.getDateFin() != null && view.getDateFin().isBefore(view.getDateDebut())) {
				errors.rejectValue("dateFin", "error.date.fin.invalide");
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
