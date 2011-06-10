package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableLong;

import ch.vd.registre.base.utils.Assert;

/**
 * Statistiques des temps de réponse par période horaire (0-1h, 1-2h, 2-3h, ...)
 */
class LoadPoint implements Comparable<LoadPoint> {

	private final Periode periode;
	private final Map<String, MutableLong> countPerUser = new HashMap<String, MutableLong>();
	private long totalCount;

	LoadPoint(Periode periode) {
		Assert.notNull(periode);
		this.periode = periode;
	}

	public void add(String user) {
		MutableLong count = countPerUser.get(user);
		if (count == null) {
			count = new MutableLong(0);
			countPerUser.put(user, count);
		}
		count.increment();
		totalCount++;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public Map<String, MutableLong> getCountPerUser() {
		return countPerUser;
	}

	public boolean isInPeriode(HourMinutes timestamp) {
		return periode.isInPeriode(timestamp);
	}

	@Override
	public int compareTo(LoadPoint o) {
		return this.periode.compareTo(o.periode);
	}

	@Override
	public String toString() {
		return String.format("%s, %d calls", periode, totalCount);
	}
}