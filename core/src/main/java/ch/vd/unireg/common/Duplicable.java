package ch.vd.unireg.common;

public interface Duplicable<T> {

	/**
	 * Crée et renvoie une copie de cet objet.
	 * 
	 * @return un duplicata de cette instance. 
	 */
	T duplicate();
}
