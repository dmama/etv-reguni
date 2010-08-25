package ch.vd.uniregctb.validation;

import java.io.Serializable;
import java.util.List;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.validation.SubValidateable;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EntityKey;
import ch.vd.uniregctb.hibernate.interceptor.AbstractLinkedInterceptor;

public class ValidationInterceptor extends AbstractLinkedInterceptor {

	private static class Behavior {
		public boolean enabled = true;
	}

	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();
	private HibernateTemplate hibernateTemplate;

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		if (entity instanceof Validateable) {
			validate((Validateable) entity);
		}
		return false;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		if (entity instanceof Validateable) {
			validate((Validateable) entity);
		}
		return false;
	}

	private void validate(Validateable object) throws ValidationException {

		final ValidationResults results = object.validate();
		if (results.hasErrors()) {
			throw new ValidationException(object, results.getErrors(), results.getWarnings());
		}

		// si l'objet possède un maître, on le valide aussi
		if (object instanceof SubValidateable) {
			Validateable master = ((SubValidateable) object).getMaster();
			if (master != null) {
				validate(master);
			}
		}

		// si l'objet pointe vers d'autres objets validables, on les valide aussi
		if (object instanceof JoinValidateable) {
			final List<EntityKey> keys = ((JoinValidateable) object).getJoinedEntities();
			for (EntityKey k : keys) {
				Validateable entity = (Validateable) hibernateTemplate.get(k.getClazz(), (Serializable) k.getId());
				if (entity != null) {
					validate(entity);
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

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}
}
