package ch.vd.unireg.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.tiers.view.ChangeDateExerciceCommercialView;

public class ChangeDateExerciceCommercialViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ChangeDateExerciceCommercialView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final ChangeDateExerciceCommercialView view = (ChangeDateExerciceCommercialView) target;

			// la nouvelle date ne doit pas être vide
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("nouvelleDate")) {
				if (view.getNouvelleDate() == null) {
					errors.rejectValue("nouvelleDate", "error.date.obligatoire");
				}
			}
		}
	}
}
