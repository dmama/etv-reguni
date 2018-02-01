package ch.vd.unireg.evenement.reqdes;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ReqDesCriteriaViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ReqDesCriteriaView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ReqDesCriteriaView view = (ReqDesCriteriaView) target;
		// TODO à compléter...
	}
}
