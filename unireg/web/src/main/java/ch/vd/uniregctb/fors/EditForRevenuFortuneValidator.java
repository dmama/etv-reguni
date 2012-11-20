package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

public abstract class EditForRevenuFortuneValidator extends EditForValidator {
	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final EditForRevenuFortuneView view = (EditForRevenuFortuneView) target;

		// validation du motif de fin
		if (view.getDateFin() != null && view.getMotifFin() == null) {
			errors.rejectValue("motifFin", "error.motif.fermeture.vide");
		}
	}
}
