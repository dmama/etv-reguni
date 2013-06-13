package ch.vd.moscow.controller.graph;

import java.util.Date;

public class TimeKey implements Comparable<TimeKey> {

	final Date time;
	final String label;

	public TimeKey(Date time, String label) {
		this.time = time;
		this.label = label;
	}

	public Date getTime() {
		return time;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public int compareTo(TimeKey o) {
		return time.compareTo(o.time);
	}

	@Override
	public String toString() {
		return label;
	}
}
