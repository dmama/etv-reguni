package ch.vd.uniregctb.webservices.tiers2.stats;

import ch.vd.registre.base.utils.Assert;

/**
 * Statistiques des temps de réponse par plage de temps de réponse (1-4 ms, 5-9 ms, 10-19 ms, ...).
 */
class ResponseTimeRange implements Comparable<ResponseTimeRange> {

	static final TimeRange[] DEFAULT_TIME_RANGES =
			{new TimeRange(0, 0), new TimeRange(1, 4), new TimeRange(5, 9), new TimeRange(10, 19), new TimeRange(20, 29), new TimeRange(30, 39), new TimeRange(40, 49), new TimeRange(50, 74),
					new TimeRange(75, 99), new TimeRange(100, 124), new TimeRange(125, 149), new TimeRange(150, 199), new TimeRange(200, 249), new TimeRange(250, 299), new TimeRange(300, 349),
					new TimeRange(350, 399), new TimeRange(400, 459), new TimeRange(450, 499), new TimeRange(500, 749), new TimeRange(720, 999), new TimeRange(1000, 1499),
					new TimeRange(1500, 1999), new TimeRange(2000, 4999), new TimeRange(5000, 9999), new TimeRange(10000, 29999), new TimeRange(30000, 59999),
					new TimeRange(60000, Long.MAX_VALUE)};

	private final TimeRange range;
	private long count;

	ResponseTimeRange(TimeRange range) {
		Assert.notNull(range);
		this.range = range;
	}

	public void incCount() {
		count++;
	}

	public long getCount() {
		return count;
	}

	public boolean isInRange(long milliseconds) {
		return range.isInRange(milliseconds);
	}

	public int compareTo(ResponseTimeRange o) {
		return this.range.compareTo(o.range);
	}

	@Override
	public String toString() {
		return String.format("%s, %d", range, count);
	}
}
