package ch.vd.uniregctb.common;

/**
 * Classe de base des containers de résultats pour les rapports d'exécution des batchs
 */
public abstract class AbstractJobResults<E, R extends AbstractJobResults> implements BatchResults<E, R> {

	/**
	 * Heure de démarrage du job (à la milliseconde près).
	 */
	public final long startTime = System.currentTimeMillis();

	/**
	 * Heure d'arrêt du job (à la milliseconde près).
	 */
	public long endTime = 0;

	public void end() {
		this.endTime = System.currentTimeMillis();
	}
}
