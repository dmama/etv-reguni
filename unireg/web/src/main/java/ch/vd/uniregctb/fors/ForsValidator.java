package ch.vd.uniregctb.fors;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;

public class ForsValidator implements Validator, InitializingBean {

	private ServiceInfrastructureService infraService;
	private AutorisationManager autorisationManager;
	private HibernateTemplate hibernateTemplate;

	private Map<Class<?>, Validator> subValidators = new HashMap<Class<?>, Validator>();

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		subValidators.put(AddForPrincipalView.class, new AddForPrincipalValidator(infraService, hibernateTemplate, autorisationManager));
		subValidators.put(AddForSecondaireView.class, new AddForSecondaireValidator(infraService, hibernateTemplate));
		subValidators.put(AddForAutreElementImposableView.class, new AddForAutreElementImposableValidator(infraService, hibernateTemplate));
		subValidators.put(AddForAutreImpotView.class, new AddForAutreImpotValidator(infraService));
		subValidators.put(AddForDebiteurView.class, new AddForDebiteurValidator(infraService, hibernateTemplate));
		subValidators.put(EditForPrincipalView.class, new EditForPrincipalValidator(hibernateTemplate));
		subValidators.put(EditForSecondaireView.class, new EditForSecondaireValidator(hibernateTemplate));
		subValidators.put(EditForAutreElementImposableView.class, new EditForAutreElementImposableValidator(hibernateTemplate));
		subValidators.put(EditForDebiteurView.class, new EditForDebiteurValidator(hibernateTemplate));
		subValidators.put(EditModeImpositionView.class, new EditModeImpositionValidator(hibernateTemplate, autorisationManager));
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return subValidators.containsKey(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		subValidators.get(target.getClass()).validate(target, errors);
	}
}
