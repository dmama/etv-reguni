package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.view.EditAllegementFiscalView;

public class EditAllegementFiscalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditAllegementFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final EditAllegementFiscalView view = (EditAllegementFiscalView) target;

		// présence des dates et cohérence entre elles
		if (view.getDateDebut() == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}
		else if (view.getDateFin() != null && view.getDateDebut().isAfter(view.getDateFin())) {
			errors.rejectValue("dateFin", "error.date.fin.avant.debut");
		}

		// date de début dans le futur -> interdit
		final RegDate today = RegDate.get();
		if (view.getDateDebut() != null && today.isBefore(view.getDateDebut())) {
			errors.rejectValue("dateDebut", "error.date.debut.future");
		}
	}
}
