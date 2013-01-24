package ch.vd.uniregctb.common;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface BatchResults<E, R extends BatchResults> {

	/**
	 * Ajoute aux résultats une exeption qui a provoqué le rollback d'une transaction.
	 *
	 * @param element l'élément processé au moment du rollback
	 * @param e       l'exception levée
	 */
	void addErrorException(E element, Exception e);

	/**
	 * Ajoute tous les résultats de droite aux résultats courant.
	 *
	 * @param right les résultats à ajouter
	 */
	void addAll(R right);
}
