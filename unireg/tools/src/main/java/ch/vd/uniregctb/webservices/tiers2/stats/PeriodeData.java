package ch.vd.uniregctb.webservices.tiers2.stats;

import ch.vd.registre.base.utils.Assert;

/**
 * Statistiques des temps de réponse par période horaire (0-1h, 1-2h, 2-3h, ...)
 */
class PeriodeData implements Comparable<PeriodeData> {

	static final Periode[] DEFAULT_PERIODES;

	static {
		DEFAULT_PERIODES = new Periode[24];
		for (int i = 0; i < 24; ++i) {
			DEFAULT_PERIODES[i] = new Periode(i, 0, i, 59);
		}
	}

	private final Periode periode;
	private long min = Long.MAX_VALUE;
	private long max;
	private long total;
	private long count;

	PeriodeData(Periode periode) {
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
		if (min == Long.MAX_VALUE) {
			return 0;
		}
		else {
			return min;
		}
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

	public boolean isInPeriode(HourMinutes timestamp) {
		return periode.isInPeriode(timestamp);
	}

	public int compareTo(PeriodeData o) {
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
