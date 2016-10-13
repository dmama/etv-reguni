package ch.vd.uniregctb.common;

import java.util.Objects;

/**
 * Interface qui permet de customiser la conversion d'un objet en chaîne de caractères
 * @param <T> type de l'objet à convertir
 */
public interface StringRenderer<T> {

	/**
	 * Implémentation par défaut du StringRenderer qui peut s'appliquer à tout objet en appelant sa méthode {@link Object#toString}
	 */
	StringRenderer<Object> DEFAULT = Objects::toString;

	/**
	 * @param object objet à convertir
	 * @return chaîne de caractères qui décrit l'objet
	 */
	String toString(T object);
}
