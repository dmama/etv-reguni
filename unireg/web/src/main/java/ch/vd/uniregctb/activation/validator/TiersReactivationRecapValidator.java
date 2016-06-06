package ch.vd.uniregctb.activation.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;

public class TiersReactivationRecapValidator implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TiersReactivationRecapView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object obj, Errors errors) {
		final TiersReactivationRecapView tiersReactivationRecapView = (TiersReactivationRecapView) obj;
		if (tiersReactivationRecapView.getDateReactivation() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateReactivation", "error.date.reactivation.vide");
		}
		else if (tiersReactivationRecapView.getDateReactivation().isAfter(RegDate.get())) {
			errors.rejectValue("dateReactivation", "error.date.reactivation.future");
		}
	}
}
