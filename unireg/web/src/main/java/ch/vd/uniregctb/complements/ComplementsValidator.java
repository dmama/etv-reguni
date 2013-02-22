package ch.vd.uniregctb.complements;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;

public class ComplementsValidator implements Validator, InitializingBean {

	private IbanValidator ibanValidator;
	private HibernateTemplate hibernateTemplate;

	private Map<Class<?>, Validator> subValidators = new HashMap<Class<?>, Validator>();

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		subValidators.put(ComplementsEditCommunicationsView.class, new ComplementsEditCommunicationsValidator(hibernateTemplate));
		subValidators.put(ComplementsEditCoordonneesFinancieresView.class, new ComplementsEditCoordonneesFinancieresValidator(ibanValidator, hibernateTemplate));
	}

		@Override
	public boolean supports(Class clazz) {
		return subValidators.containsKey(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		subValidators.get(target.getClass()).validate(target, errors);
	}
}
