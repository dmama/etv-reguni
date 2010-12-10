package ch.vd.uniregctb.validation;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Classe de base des validateurs, qui enregistre le validateur dans le service de validation
 * @param <T> le type d'entité validable
 */
public abstract class EntityValidatorImpl<T> implements EntityValidator<T>, InitializingBean, DisposableBean {

	private ValidationService validationService;

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	protected ValidationService getValidationService() {
		return validationService;
	}

	protected abstract Class<T> getValidatedClass();

	public void afterPropertiesSet() throws Exception {
		validationService.registerValidator(getValidatedClass(), this);
	}

	public void destroy() throws Exception {
		validationService.unregisterValidator(getValidatedClass(), this);
	}
}
