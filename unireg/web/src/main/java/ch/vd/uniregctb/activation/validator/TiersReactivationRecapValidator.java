package ch.vd.uniregctb.activation.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.activation.view.TiersReactivationRecapView;

public class TiersReactivationRecapValidator implements Validator {


	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TiersReactivationRecapView.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {

		Assert.isTrue(obj instanceof TiersReactivationRecapView);
		TiersReactivationRecapView tiersReactivationRecapView = (TiersReactivationRecapView) obj;
		if (tiersReactivationRecapView.getDateReactivation() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateReactivation", "error.date.reactivation.vide");
		}
	}
}
