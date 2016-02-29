package ch.vd.uniregctb.migration.pm.log;

import java.util.Map;
import java.util.function.BinaryOperator;

import org.jetbrains.annotations.NotNull;

public abstract class LoggedElementHelper {

	/**
	 * Exception lancée par le merger "exceptionThrowing" (voir {@link #exceptionThrowing()})
	 */
	public static class IncompabibleValuesException extends RuntimeException {
		public IncompabibleValuesException(String message) {
			super(message);
		}

		public IncompabibleValuesException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * @return un opérateur binaire qui explose dès qu'on y touche en disant que les deux valeurs sont incompatibles
	 */
	@NotNull
	public static <T> BinaryOperator<T> exceptionThrowing() {
		return (o1, o2) -> { throw new IncompabibleValuesException(String.format("Valeurs incompatibles %s // %s", o1, o2)); };
	}

	/**
	 * Helper pour ajouter une valeur à une map après vérification
	 * @param map la map cible
	 * @param newItem le nouvel item de la valeur
	 * @param newValue la valeur à ajouter
	 * @throws IllegalArgumentException si la nouvelle valeur n'est pas acceptable pour ce concept
	 */
	public static void addValue(Map<LoggedElementAttribute, Object> map, LoggedElementAttribute newItem, Object newValue) {
		checkValue(newItem, newValue);
		map.put(newItem, newValue);
	}

	/**
	 * @param item concept de la valeur
	 * @param value valeur candidate
	 * @throws IllegalArgumentException si la nouvelle valeur n'est pas acceptable pour ce concept
	 */
	public static void checkValue(LoggedElementAttribute item, Object value) {
		if (value != null) {
			final Class<?> newValueClass = value.getClass();
			final Class<?> expectedClass = item.getValueClass();
			if (!expectedClass.isAssignableFrom(newValueClass)) {
				throw new IllegalArgumentException(String.format("La valeur '%s' (classe %s) n'est pas admise pour le concept %s", value, newValueClass.getName(), item));
			}
		}
	}
}
