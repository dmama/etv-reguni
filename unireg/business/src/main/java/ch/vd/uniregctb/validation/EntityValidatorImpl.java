package ch.vd.uniregctb.validation;

import org.jetbrains.annotations.NotNull;
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

	@Override
	public void afterPropertiesSet() throws Exception {
		validationService.registerValidator(getValidatedClass(), this);
	}

	@Override
	public void destroy() throws Exception {
		validationService.unregisterValidator(getValidatedClass(), this);
	}

	/**
	 * @param entity une entité dont on veut générer une description courte et suffisante
	 * @return une destription textuelle courte de l'entité
	 */
	protected String getEntityDisplayString(@NotNull T entity) {
		return entity.toString();
	}
}
