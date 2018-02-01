package ch.vd.unireg.common;

import ch.vd.shared.batchtemplate.BatchResults;

/**
 * Classe de base des containers de résultats pour les rapports d'exécution des batchs
 */
public abstract class AbstractJobResults<E, R extends AbstractJobResults<E, R>> implements BatchResults<E, R> {

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
