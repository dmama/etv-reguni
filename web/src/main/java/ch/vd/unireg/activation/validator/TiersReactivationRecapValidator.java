package ch.vd.unireg.activation.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.activation.view.TiersReactivationRecapView;

public class TiersReactivationRecapValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TiersReactivationRecapView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		final TiersReactivationRecapView tiersReactivationRecapView = (TiersReactivationRecapView) obj;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateReactivation")) {
			if (tiersReactivationRecapView.getDateReactivation() == null) {
				ValidationUtils.rejectIfEmpty(errors, "dateReactivation", "error.date.reactivation.vide");
			}
			else if (tiersReactivationRecapView.getDateReactivation().isAfter(RegDate.get())) {
				errors.rejectValue("dateReactivation", "error.date.reactivation.future");
			}
		}
	}
}
