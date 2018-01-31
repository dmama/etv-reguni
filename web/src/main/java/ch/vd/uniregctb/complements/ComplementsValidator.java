package ch.vd.uniregctb.complements;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.iban.IbanValidator;

public class ComplementsValidator implements Validator, InitializingBean {

	private IbanValidator ibanValidator;

	private final Map<Class<?>, Validator> subValidators = new HashMap<>();

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		subValidators.put(ComplementsEditCommunicationsView.class, new ComplementsEditCommunicationsValidator());
		subValidators.put(ComplementsEditCoordonneesFinancieresView.class, new ComplementsEditCoordonneesFinancieresValidator(ibanValidator));
	}

		@Override
	public boolean supports(Class clazz) {
		return subValidators.containsKey(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		subValidators.get(target.getClass()).validate(target, errors);
	}
}
