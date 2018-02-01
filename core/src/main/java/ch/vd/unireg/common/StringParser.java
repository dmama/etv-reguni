package ch.vd.unireg.common;

/**
 * Interface qui permet de customiser la conversion d'une chaîne de caractères en objet typé
 * @param <T> type de l'objet à convertir
 */
public interface StringParser<T> {

	/**
	 * @param string string à parser
	 * @return objet construit à partir de la chaîne
	 * @throws java.lang.IllegalArgumentException en cas de problème de parsing
	 */
	T parse(String string) throws IllegalArgumentException;
}
