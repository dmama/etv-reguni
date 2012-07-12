package ch.vd.moscow.database;

import java.util.Date;
import java.util.List;

public class CallStats {

	private Number nbCalls;
	private Number accumulatedTime;
	private Number maxPing;
	private List<Object> coord;
	private Date date;

	public CallStats(Number nbCalls, Number accumulatedTime, Number maxPing, List<Object> coord, Date date) {
		this.nbCalls = nbCalls;
		this.accumulatedTime = accumulatedTime;
		this.maxPing = maxPing;
		this.coord = coord;
		this.date = date;
	}

	public int getNbCalls() {
		return nbCalls.intValue();
	}

	public long getAccumulatedTime() {
		return accumulatedTime.longValue();
	}

	public long getMaxPing() {
		return maxPing.longValue();
	}

	public List<Object> getCoord() {
		return coord;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void merge(CallStats old) {
		this.nbCalls = this.nbCalls.intValue() + old.nbCalls.intValue();
		this.accumulatedTime = this.accumulatedTime.longValue() + old.accumulatedTime.longValue();
		this.maxPing = Math.max(this.maxPing.longValue(), old.maxPing.longValue());
	}
}
