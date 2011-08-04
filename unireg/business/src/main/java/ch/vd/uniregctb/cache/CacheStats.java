package ch.vd.uniregctb.cache;

/**
 * Expose les statistiques de fonctionnement d'un cache.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface CacheStats {

	/**
	 * @return le percentage de hits sur le cache du service; ou <b>null</b> si cette information n'est pas disponible
	 */
	public Long getHitsPercent();

	/**
	 * @return le nombre de hits sur le cache du service
	 */
	public long getHitsCount();

	/**
	 * @return le nombre d'appels sur le cache du service
	 */
	public long getTotalCount();

	/**
	 * @return time-to-idle en secondes; ou <b>null</b> si cette information n'est pas applicable
	 */
	public Long getTimeToIdle();

	/**
	 * @return time-to-live en secondes; ou <b>null</b> si cette information n'est pas applicable
	 */
	public Long getTimeToLive();

	/**
	 * @return nombre maximum d'éléments en mémoire; ou <b>null</b> si cette information n'est pas applicable
	 */
	public Integer getMaxElements();

	public String toString();
}