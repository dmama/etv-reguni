package ch.vd.unireg.cache;

/**
 * Expose les statistiques de fonctionnement d'un cache.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface CacheStats {

	/**
	 * @return le percentage de hits sur le cache du service; ou <b>null</b> si cette information n'est pas disponible
	 */
	Long getHitsPercent();

	/**
	 * @return le nombre de hits sur le cache du service
	 */
	long getHitsCount();

	/**
	 * @return le nombre d'appels sur le cache du service
	 */
	long getTotalCount();

	/**
	 * @return time-to-idle en secondes; ou <b>null</b> si cette information n'est pas applicable
	 */
	Long getTimeToIdle();

	/**
	 * @return time-to-live en secondes; ou <b>null</b> si cette information n'est pas applicable
	 */
	Long getTimeToLive();

	/**
	 * @return nombre maximum d'éléments en mémoire; ou <b>null</b> si cette information n'est pas applicable
	 */
	Long getMaxElements();

	String toString();
}