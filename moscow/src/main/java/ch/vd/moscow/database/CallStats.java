package ch.vd.moscow.database;

import java.util.Date;
import java.util.List;

public class CallStats {

	private Number nbCalls;
	private List<Object> coord;
	private Date date;

	public CallStats(Number nbCalls, List<Object> coord, Date date) {
		this.nbCalls = nbCalls;
		this.coord = coord;
		this.date = date;
	}

	public int getNbCalls() {
		return nbCalls.intValue();
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
	}
}
