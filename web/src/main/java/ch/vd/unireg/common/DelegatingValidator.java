package ch.vd.unireg.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public abstract class DelegatingValidator implements Validator {

	private final Map<Class<?>, Validator> subValidators = new HashMap<>();

	@Override
	public final boolean supports(Class<?> clazz) {
		return subValidators.containsKey(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		subValidators.get(target.getClass()).validate(target, errors);
	}

	/**
	 * Utilisé par les sous-classes pour enregistrer des sous-validateur
	 * @param clazz classe prise en compte par le validateur
	 * @param validator validateur associé à la classe
	 */
	protected final void addSubValidator(Class<?> clazz, Validator validator) {
		subValidators.put(clazz, validator);
	}

	protected static class DummyValidator<T> implements Validator {

		private final Class<T> clazz;

		public DummyValidator(Class<T> clazz) {
			this.clazz = clazz;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return this.clazz.equals(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			// ne fait rien... c'est ça, le Dummy, dans le nom...
		}
	}
}
