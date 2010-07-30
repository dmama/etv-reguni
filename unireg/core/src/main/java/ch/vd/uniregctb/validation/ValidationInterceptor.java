package ch.vd.uniregctb.validation;

import java.io.Serializable;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;

import ch.vd.registre.base.validation.SubValidateable;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.hibernate.interceptor.AbstractLinkedInterceptor;

public class ValidationInterceptor extends AbstractLinkedInterceptor {

	private static class Behavior {
		public boolean enabled = true;
	}

	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();

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
}
