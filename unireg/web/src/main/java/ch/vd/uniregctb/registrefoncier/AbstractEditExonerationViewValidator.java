package ch.vd.uniregctb.registrefoncier;

import java.math.BigDecimal;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class AbstractEditExonerationViewValidator implements Validator {

	private static final BigDecimal CENT = BigDecimal.valueOf(100L);

	@Override
	public boolean supports(Class<?> clazz) {
		return AbstractEditExonerationView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AbstractEditExonerationView view = (AbstractEditExonerationView) target;

		// la période de début est obligatoire
		if (view.getPfDebut() == null && !errors.hasFieldErrors("pfDebut")) {
			errors.rejectValue("pfDebut", "error.champ.obligatoire");
		}

		// le pourcentage doit être compris entre 0 et 100, avec deux décimales
		final BigDecimal pourcentage = view.getPourcentageExoneration();
		if (pourcentage == null) {
			if (!errors.hasFieldErrors("pourcentageExoneration")) {
				errors.rejectValue("pourcentageExoneration", "error.champ.obligatoire");
			}
		}
		else {
			if (pourcentage.compareTo(CENT) > 0 || pourcentage.compareTo(BigDecimal.ZERO) < 0) {
				errors.rejectValue("pourcentageExoneration", "error.degexo.pourcentage.hors.limites");
			}
		}
	}
}
