package ch.vd.uniregctb.common;

/**
 * Interface qui permet de comparer des objets en ne tenant compte que des informations métier.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface BusinessComparable<T> {

	/**
	 * Retourne true si l'objet est de même type contient les mêmes informations business que celui passé en paramètre.
	 *
	 * Cette méthode ne doit pas être renommée en equals, cela provoquerait des conflits avec Hibernate.
	 */
	public boolean equalsTo(T obj);

}
