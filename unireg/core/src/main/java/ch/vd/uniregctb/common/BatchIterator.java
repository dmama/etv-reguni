package ch.vd.uniregctb.common;

import java.util.List;

/**
 * Interface de l'itérateur sur lots. Elle définit grosso-modo la même genre d'interface que {@link java.util.Iterator}, mais avec une sémantique légèrement différente pour permettre des
 * implémentations créatives
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface BatchIterator<E> {

	/**
	 * <b>Note:</b> cette méthode n'a qu'une fonction indicative, elle ne dispense pas l'appelant de vérifier le retour de {@link #next()} qui détermine réellement s'il y a quelque chose à traiter ou
	 * non.
	 *
	 * @return <i>vrai</i> s'il y a encore un lot à traiter; ou <i>faux</i> autrement.
	 */
	boolean hasNext();

	/**
	 * @return le prochain lot à traiter; ou <i>null</i> s'il n'y a plus rien à faire.
	 */
	List<E> next();

	/**
	 * @return le pourcentage de progression de l'itération.
	 */
	int getPercent();
}
