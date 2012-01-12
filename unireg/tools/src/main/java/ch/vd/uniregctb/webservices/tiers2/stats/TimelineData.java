package ch.vd.uniregctb.webservices.tiers2.stats;

import ch.vd.registre.base.utils.Assert;

/**
 * Statistiques des temps de réponse par période horaire (0-1h, 1-2h, 2-3h, ...)
 */
class TimelineData implements Comparable<TimelineData> {

	private final Periode periode;
	private long min = Long.MAX_VALUE;
	private long max;
	private long total;
	private long count;

	TimelineData(Periode periode) {
		Assert.notNull(periode);
		this.periode = periode;
	}

	public void add(long milli) {
		count++;
		total += milli;
		min = Math.min(min, milli);
		max = Math.max(max, milli);
	}

	public long getMin() {
		return min;
	}

	public long getMax() {
		return max;
	}

	public long getAverage() {
		if (count > 0) {
			return total / count;
		}
		else {
			return 0;
		}
	}

	public long getTotal() {
		return total;
	}

	public long getCount() {

		return count;
	}

	public boolean isInPeriode(HourMinutes timestamp) {
		return periode.isInPeriode(timestamp);
	}

	@Override
	public int compareTo(TimelineData o) {
		return this.periode.compareTo(o.periode);
	}

	@Override
	public String toString() {
		if (min == Long.MAX_VALUE) {
			return String.format("%s, - ms (min - ms, max - ms)", periode);
		}
		else {
			return String.format("%s, %d ms (min %d ms, max %d ms)", periode, getAverage(), min, max);
		}
	}
}
