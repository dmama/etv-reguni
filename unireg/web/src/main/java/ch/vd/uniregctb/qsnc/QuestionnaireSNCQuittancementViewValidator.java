package ch.vd.uniregctb.qsnc;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class QuestionnaireSNCQuittancementViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return QuestionnaireSNCQuittancementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final QuestionnaireSNCQuittancementView view = (QuestionnaireSNCQuittancementView) target;
		if (view.getDateRetour() == null) {
			errors.rejectValue("dateRetour", "error.date.retour.vide");
		}
		else if (RegDate.get().isBefore(view.getDateRetour())) {
			errors.rejectValue("dateRetour", "error.date.retour.future");
		}
		else if (view.getDateEmission() != null && view.getDateRetour().isBefore(view.getDateEmission())) {
			errors.rejectValue("dateRetour", "error.date.retour.anterieure.date.emission");
		}
	}
}
