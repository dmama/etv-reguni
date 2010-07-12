package ch.vd.uniregctb.stats;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;

/**
 * Contient les statistiques de fonctionnement d'un cache.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CacheStats {

	private Long hitsPercent;
	private Long hitsCount;
	private Long totalCount;
	private long timeToIdle;
	private long timeToLive;
	private int maxElements;

	public CacheStats(Ehcache cache) {

		timeToIdle = cache.getTimeToIdleSeconds();
		timeToLive = cache.getTimeToLiveSeconds();
		maxElements = cache.getMaxElementsInMemory();

		final Statistics statistics = cache.getStatistics();
		final long hits = statistics.getCacheHits();
		final long misses = statistics.getCacheMisses();
		final long total = hits + misses;

		this.hitsCount = hits;
		this.totalCount = total;
		if (total > 0) {
			this.hitsPercent = (hits * 100) / total;
		}
	}

	/**
	 * @return le percentage de hits sur le cache du service
	 */
	public Long getHitsPercent() {
		return hitsPercent;
	}

	/**
	 * @return le nombre de hits sur le cache du service
	 */
	public Long getHitsCount() {
		return hitsCount;
	}

	/**
	 * @return le nombre d'appels sur le cache du service
	 */
	public Long getTotalCount() {
		return totalCount;
	}

	/**
	 * @return time-to-idle en secondes
	 */
	public long getTimeToIdle() {
		return timeToIdle;
	}

	/**
	 * @return time-to-live en secondes
	 */
	public long getTimeToLive() {
		return timeToLive;
	}

	/**
	 * @return nombre maximum d'éléments en mémoire
	 */
	public int getMaxElements() {
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