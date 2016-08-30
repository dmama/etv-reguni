package ch.vd.unireg.interfaces.organisation.rcent.converters;

/**
 * Fourni une fonction de conversion pour convertir une valeur en une autre.
 * @param <T> Le type de la valeur en entrée.
 * @param <R> Le type de la valeur en sortie.
 */
public interface Converter<T, R> {
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	R apply(T t);
}
