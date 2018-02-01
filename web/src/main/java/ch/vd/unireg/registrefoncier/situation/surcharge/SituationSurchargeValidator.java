package ch.vd.uniregctb.registrefoncier.situation.surcharge;


import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class SituationSurchargeValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SituationSurchargeView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final SituationSurchargeView view =(SituationSurchargeView) target;

		if (view.getNoOfsSurcharge() == null) {
			errors.rejectValue("noOfsSurcharge", "error.fraction.commune.obligatoire");
		}
	}
}
