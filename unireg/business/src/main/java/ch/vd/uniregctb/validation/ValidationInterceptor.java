package ch.vd.uniregctb.validation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.hibernate.interceptor.AbstractLinkedInterceptor;
import ch.vd.uniregctb.tiers.LinkedEntity;

public class ValidationInterceptor extends AbstractLinkedInterceptor {

	private static class Behavior {
		public boolean enabled = true;
	}

	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();
	private HibernateTemplate hibernateTemplate;
	private ValidationService validationService;

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		validate(entity);
		return false;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		validate(entity);
		return false;
	}

	/**
	 * Valide l'objet spécifié et tous les objets liés qui peuvent l'être.
	 *
	 * @param object l'objet à valider
	 * @throws ValidationException en cas d'erreur de validation
	 */
	private void validate(Object object) throws ValidationException {
		final Set<Object> visited = new HashSet<Object>();
		validate(object, visited);
	}

	private void validate(Object object, Set<Object> visited) {

		if (visited.contains(object)) {
			return;
		}

		final ValidationResults results = validationService.validate(object);
		if (results.hasErrors()) {
			throw new ValidationException(object, results.getErrors(), results.getWarnings());
		}

		visited.add(object);

		// si l'objet pointe vers d'autres objets validables, on les valide aussi
		if (object instanceof LinkedEntity) {
			final LinkedEntity entity =(LinkedEntity) object;
			final List<?> linked = entity.getLinkedEntities();
			if (linked != null) {
				for (Object o : linked) {
					if (o instanceof EntityKey) {
						final EntityKey k = (EntityKey) o;
						o = hibernateTemplate.get(k.getClazz(), (Serializable) k.getId());
					}
					validate(o, visited);
				}
			}
		}
	}

	private Behavior getByThreadBehavior() {
		Behavior behavior = this.byThreadBehavior.get();
		if (behavior == null) {
			behavior = new Behavior();
			this.byThreadBehavior.set(behavior);
		}
		return behavior;
	}

	public void setEnabled(boolean value) {
		getByThreadBehavior().enabled = value;
	}

	public boolean isEnabled() {
		return getByThreadBehavior().enabled;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}
}
