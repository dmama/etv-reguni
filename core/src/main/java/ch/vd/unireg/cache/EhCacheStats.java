package ch.vd.unireg.cache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.StatisticsGateway;

/**
 * Expose les statistiques de fonctionnement d'un ehcache.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EhCacheStats implements CacheStats {

	private final Long hitsPercent;
	private final long hitsCount;
	private final long totalCount;
	private final long timeToIdle;
	private final long timeToLive;
	private final long maxElements;

	public EhCacheStats(Ehcache cache) {

		timeToIdle = cache.getCacheConfiguration().getTimeToIdleSeconds();
		timeToLive = cache.getCacheConfiguration().getTimeToLiveSeconds();
		maxElements = cache.getCacheConfiguration().getMaxEntriesLocalHeap();

		final StatisticsGateway statistics = cache.getStatistics();
		final long hits = statistics.cacheHitCount();
		final long misses = statistics.cacheMissCount();
		final long total = hits + misses;

		this.hitsCount = hits;
		this.totalCount = total;
		if (total > 0) {
			this.hitsPercent = (hits * 100) / total;
		}
		else {
			this.hitsPercent = null;
		}
	}

	/**
	 * @return le percentage de hits sur le cache du service
	 */
	@Override
	public Long getHitsPercent() {
		return hitsPercent;
	}

	/**
	 * @return le nombre de hits sur le cache du service
	 */
	@Override
	public long getHitsCount() {
		return hitsCount;
	}

	/**
	 * @return le nombre d'appels sur le cache du service
	 */
	@Override
	public long getTotalCount() {
		return totalCount;
	}

	/**
	 * @return time-to-idle en secondes
	 */
	@Override
	public Long getTimeToIdle() {
		return timeToIdle;
	}

	/**
	 * @return time-to-live en secondes
	 */
	@Override
	public Long getTimeToLive() {
		return timeToLive;
	}

	/**
	 * @return nombre maximum d'éléments en mémoire
	 */
	@Override
	public Long getMaxElements() {
		return maxElements;
	}

	@Override
	public String toString() {
		return "hitsPercent=" + hitsPercent +
				", hitsCount=" + hitsCount +
				", totalCount=" + totalCount +
				", timeToIdle=" + timeToIdle +
				", timeToLive=" + timeToLive +
				", maxElements=" + maxElements;
	}
}