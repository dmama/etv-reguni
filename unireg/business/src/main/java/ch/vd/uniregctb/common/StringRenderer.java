package ch.vd.uniregctb.common;

/**
 * Interface qui permet de customiser la conversion d'un objet en chaîne de caractère
 * @param <T> type de l'objet à convertir
 */
public interface StringRenderer<T> {

	/**
	 * @param object objet à convertir
	 * @return chaîne de caractères qui décrit l'objet
	 */
	String toString(T object);
}
