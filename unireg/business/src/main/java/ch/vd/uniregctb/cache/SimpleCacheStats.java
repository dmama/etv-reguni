package ch.vd.uniregctb.cache;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleCacheStats implements CacheStats {

	private final AtomicLong hitsCount;
	private final AtomicLong missCount;

	public SimpleCacheStats() {
		this.hitsCount = new AtomicLong();
		this.missCount = new AtomicLong();
	}

	public SimpleCacheStats(SimpleCacheStats right) {
		this.hitsCount = new AtomicLong(right.hitsCount.longValue());
		this.missCount = new AtomicLong(right.missCount.longValue());
	}

	public void addHit() {
		hitsCount.incrementAndGet();
	}

	public void addMiss() {
		missCount.incrementAndGet();
	}

	public Long getHitsPercent() {
		final long total = hitsCount.longValue() + missCount.longValue();
		if (total > 0) {
			return hitsCount.longValue() * 100L / total;
		}
		else {
			return null;
		}
	}

	public long getHitsCount() {
		return hitsCount.longValue();
	}

	public long getTotalCount() {
		return hitsCount.longValue() + missCount.longValue();
	}

	public Long getTimeToIdle() {
		return null;
	}

	public Long getTimeToLive() {
		return null;
	}

	public Integer getMaxElements() {
		return null;
	}

	@Override
	public String toString() {
		return "hitsPercent=" + getHitsPercent() +
				", hitsCount=" + getHitsCount() +
				", totalCount=" + getTotalCount();
	}
}
