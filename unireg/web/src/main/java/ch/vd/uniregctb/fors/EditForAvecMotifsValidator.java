package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

public abstract class EditForAvecMotifsValidator extends EditForValidator {

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final EditForAvecMotifsView view = (EditForAvecMotifsView) target;

		// [SIFISC-7381] les dates et motifs doivent être renseignés tous les deux, ou nuls tous les deux
		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (view.getMotifDebut() != null && view.getDateDebut() == null) {
				errors.rejectValue("dateDebut", "error.date.ouverture.vide");
			}
		}
		// [SIFISC-18086] blindage en cas de mauvais format de date, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFin")) {
			if (view.getMotifFin() != null && view.getDateFin() == null) {
				errors.rejectValue("dateFin", "error.date.fermeture.vide");
			}
		}

		// validation du motif de fin
		if (view.getDateFin() != null && view.getMotifFin() == null) {
			errors.rejectValue("motifFin", "error.motif.fermeture.vide");
		}
	}
}
