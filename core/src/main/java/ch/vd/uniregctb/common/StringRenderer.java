package ch.vd.uniregctb.common;

import java.util.Objects;

/**
 * Interface qui permet de customiser la conversion d'un objet en chaîne de caractères
 * @param <T> type de l'objet à convertir
 */
@FunctionalInterface
public interface StringRenderer<T> {

	/**
	 * Implémentation par défaut du StringRenderer qui peut s'appliquer à tout objet en appelant sa méthode {@link Object#toString}
	 */
	StringRenderer<Object> DEFAULT = Objects::toString;

	/**
	 * Constructeur d'une façade qui permet de traiter le cas de l'argument <code>null</code> même si le renderer de base ne le supporte pas
	 * @param ifNull valeur à utiliser si l'argument est <code>null</code>
	 * @param ifNonNull renderer à utiliser si l'argument est non-nulle
	 * @param <U> type de l'argument
	 * @return un renderer capable de traiter les cas <code>null</code>
	 */
	static <U> StringRenderer<U> withDefaultIfNull(String ifNull, StringRenderer<U> ifNonNull) {
		Objects.requireNonNull(ifNonNull);
		return u -> u == null ? ifNull : ifNonNull.toString(u);
	}

	/**
	 * @param object objet à convertir
	 * @return chaîne de caractères qui décrit l'objet
	 */
	String toString(T object);
}
