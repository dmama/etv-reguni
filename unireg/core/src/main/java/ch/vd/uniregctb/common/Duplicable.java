package ch.vd.uniregctb.common;

import java.io.Serializable;

public interface Duplicable<T> extends Serializable {

	/**
	 * Crée et renvoie une copie de cet objet.
	 * 
	 * @return un duplicata de cette instance. 
	 */
	T duplicate();
	
}
