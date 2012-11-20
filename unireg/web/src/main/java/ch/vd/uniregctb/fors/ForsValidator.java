package ch.vd.uniregctb.fors;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * FIXME (msi) reprendre les règles de validation du ForFiscalViewValidator
 */
public class ForsValidator implements Validator, InitializingBean {

	private ServiceInfrastructureService infraService;
	private TiersDAO tiersDAO;
	private Map<Class<?>, Validator> subValidators = new HashMap<Class<?>, Validator>();

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		subValidators.put(AddForPrincipalView.class, new AddForPrincipalValidator(infraService));
		subValidators.put(AddForSecondaireView.class, new AddForSecondaireValidator(infraService));
		subValidators.put(AddForAutreElementImposableView.class, new AddForAutreElementImposableValidator(infraService));
		subValidators.put(AddForAutreImpotView.class, new AddForAutreImpotValidator(infraService));
		subValidators.put(AddForDebiteurView.class, new AddForDebiteurValidator(infraService, tiersDAO));
		subValidators.put(EditForPrincipalView.class, new EditForPrincipalValidator());
		subValidators.put(EditForSecondaireView.class, new EditForSecondaireValidator());
		subValidators.put(EditForAutreElementImposableView.class, new EditForAutreElementImposableValidator());
		subValidators.put(EditForDebiteurView.class, new EditForDebiteurValidator(tiersDAO));
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
