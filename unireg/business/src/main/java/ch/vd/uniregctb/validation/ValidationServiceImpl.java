package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LockHelper;

/**
 * Service de validation des entités
 */
public class ValidationServiceImpl implements ValidationService {

	private final Map<Class, EntityValidator> validatorMap = new HashMap<>();

	private final LockHelper lockHelper = new LockHelper();
	private final ThreadLocal<MutableInt> callDepth = ThreadLocal.withInitial(() -> new MutableInt(0));

	/**
	 * Façade de validateur qui permet de chaîner plusieurs validateurs sur une même classe
	 * @param <T>
	 */
	private static class ChainingValidator<T> implements EntityValidator<T> {

		private final List<EntityValidator<T>> chainedValidators = new ArrayList<>();

		public void addValidator(EntityValidator<T> validator) {
			chainedValidators.add(validator);
		}

		public void removeValidator(EntityValidator<T> validator) {
			chainedValidators.remove(validator);
		}

		public boolean isEmpty() {
			return chainedValidators.isEmpty();
		}

		@Override
		public ValidationResults validate(T entity) {
			final ValidationResults results = new ValidationResults();
			for (EntityValidator<T> validator : chainedValidators) {
				results.merge(validator.validate(entity));
			}
			return results;
		}
	}

	@Override
	public <T> void registerValidator(Class<T> clazz, EntityValidator<T> validator) {
		lockHelper.doInWriteLock(() -> addValidator(clazz, validator));
	}

	@Override
	public <T> void unregisterValidator(Class<T> clazz, EntityValidator<T> validator) {
		lockHelper.doInWriteLock(() -> removeValidator(clazz, validator));
	}

	@SuppressWarnings({"unchecked"})
	private <T> void addValidator(Class<T> clazz, EntityValidator<T> validator) {
		// détection des doublons
		final EntityValidator existingValidator = validatorMap.get(clazz);
		if (existingValidator != null) {
			if (existingValidator instanceof ChainingValidator) {
				((ChainingValidator) existingValidator).addValidator(validator);
			}
			else {
				final ChainingValidator chainingValidator = new ChainingValidator();
				chainingValidator.addValidator(existingValidator);
				chainingValidator.addValidator(validator);
				validatorMap.put(clazz, chainingValidator);
			}
		}
		else {
			validatorMap.put(clazz, validator);
		}
	}

	@SuppressWarnings({"unchecked"})
	private <T> void removeValidator(Class<T> clazz, EntityValidator<T> validator) {
		final EntityValidator foundValidator = validatorMap.get(clazz);
		if (foundValidator == validator) {
			validatorMap.remove(clazz);
		}
		else if (foundValidator instanceof ChainingValidator) {
			final ChainingValidator chaining = (ChainingValidator) foundValidator;
			chaining.removeValidator(validator);
			if (chaining.isEmpty()) {
				validatorMap.remove(clazz);
			}
		}
	}

	protected EntityValidator findValidator(Object object) {
		if (object == null) {
			return null;
		}

		return lockHelper.doInReadLock(() -> {
			EntityValidator validator = null;
			Class clazz = object.getClass();
			while (validator == null && clazz != null) {
				validator = validatorMap.get(clazz);
				clazz = clazz.getSuperclass();
			}
			return validator;
		});
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public ValidationResults validate(Object object) {
		final MutableInt depth = getCallDepth();
		try {
			depth.increment();
			final EntityValidator validator = findValidator(object);
			if (validator != null) {
				return validator.validate(object);
			}
			else {
				return new ValidationResults();
			}
		}
		finally {
			depth.decrement();
		}
	}

	private MutableInt getCallDepth() {
		return callDepth.get();
	}

	@Override
	public boolean isInValidation() {
		return getCallDepth().intValue() > 0;
	}
}
