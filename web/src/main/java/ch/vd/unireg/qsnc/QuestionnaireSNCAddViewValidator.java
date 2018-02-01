package ch.vd.unireg.qsnc;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class QuestionnaireSNCAddViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return QuestionnaireSNCAddView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final QuestionnaireSNCAddView view = (QuestionnaireSNCAddView) target;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("delaiAccorde")) {
			if (view.getDelaiAccorde() == null) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.vide");
			}
			else if (RegDate.get().isAfterOrEqual(view.getDelaiAccorde())) {
				errors.rejectValue("delaiAccorde", "error.delai.accorde.invalide");
			}
		}
	}
}
