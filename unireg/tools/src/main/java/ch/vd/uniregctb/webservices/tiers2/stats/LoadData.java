package ch.vd.uniregctb.webservices.tiers2.stats;

import ch.vd.registre.base.utils.Assert;

/**
 * Statistiques des temps de réponse par période horaire (0-1h, 1-2h, 2-3h, ...)
 */
class LoadData implements Comparable<LoadData> {

	private final Periode periode;
	private long count;

	LoadData(Periode periode) {
		Assert.notNull(periode);
		this.periode = periode;
	}

	public void add() {
		count++;
	}

	public long getCount() {
		return count;
	}

	public boolean isInPeriode(HourMinutes timestamp) {
		return periode.isInPeriode(timestamp);
	}

	public int compareTo(LoadData o) {
		return this.periode.compareTo(o.periode);
	}

	@Override
	public String toString() {
		return String.format("%s, %d calls", periode, count);
	}
}