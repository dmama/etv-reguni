package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public abstract class EditForValidator implements Validator {
	@Override
	public void validate(Object target, Errors errors) {
		final EditForView view = (EditForView) target;

		// validation de la date de fin
		final RegDate dateDebut = view.getDateDebut();
		final RegDate dateFin = view.getDateFin();
		if (dateFin != null) {
			if (RegDate.get().isBefore(dateFin) && !view.isDateFinFutureAutorisee()) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
			else if (dateDebut != null && dateFin.isBefore(dateDebut)) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
		}

		if (view.getNoAutoriteFiscale() == null) {
			errors.rejectValue("noAutoriteFiscale", "error.autorite.fiscale.vide");
		}
	}
}
