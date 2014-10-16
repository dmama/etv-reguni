package ch.vd.uniregctb.webservices.tiers2.stats;

class TimeRange implements Comparable<TimeRange> {

	private final long start;
	private final long end;

	TimeRange(long start, long end) {
		this.start = start;
		this.end = end;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public boolean isInRange(long milliseconds) {
		return start <= milliseconds && milliseconds <= end;
	}

	@Override
	public int compareTo(TimeRange o) {
		return (start < o.start ? -1 : (start == o.start ? 0 : 1));
	}

	@Override
	public String toString() {
		if (end == Long.MAX_VALUE) {
			return String.format(">= %d ms", start);
		}
		else {
			return String.format("%d-%d ms", start, end);
		}
	}
}
