package ch.vd.unireg.validation;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Classe de base des validateurs, qui enregistre le validateur dans le service de validation
 * @param <T> le type d'entité validable
 */
public abstract class EntityValidatorImpl<T> implements EntityValidator<T>, InitializingBean, DisposableBean {

	private ValidationService validationService;
	private ValidableEntityNamingService entityNamingService;

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setEntityNamingService(ValidableEntityNamingService entityNamingService) {
		this.entityNamingService = entityNamingService;
	}

	protected ValidationService getValidationService() {
		return validationService;
	}

	protected ValidableEntityNamingService getEntityNamingService() {
		return entityNamingService;
	}

	protected abstract Class<T> getValidatedClass();

	@Override
	public void afterPropertiesSet() throws Exception {
		validationService.registerValidator(getValidatedClass(), this);
		entityNamingService.registerEntityRenderer(getValidatedClass(), this::getEntityDisplayString);
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
