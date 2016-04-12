package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.tiers.view.ChangeDateExerciceCommercialView;

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
			if (view.getNouvelleDate() == null) {
				errors.rejectValue("nouvelleDate", "error.date.obligatoire");
			}
		}
	}
}
