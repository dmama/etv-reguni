package ch.vd.uniregctb.tiers.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.tiers.view.DebiteurFiscalView;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class DebiteurFiscalViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return DebiteurFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final DebiteurFiscalView view = (DebiteurFiscalView) target;

		if (view.getCategorieImpotSource() == null) {
			errors.rejectValue("categorieImpotSource", "error.champ.obligatoire");
		}
		if (view.getModeCommunication() == null) {
			errors.rejectValue("modeCommunication", "error.champ.obligatoire");
		}
		if (view.getPeriodiciteDecompte() == null) {
			errors.rejectValue("periodiciteDecompte", "error.champ.obligatoire");
		}
		else if (view.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE && view.getPeriodeDecompte() == null) {
			errors.rejectValue("periodeDecompte", "error.champ.obligatoire");
		}
	}
}
