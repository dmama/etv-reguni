package ch.vd.uniregctb.webservices.tiers2.stats;

class Periode implements Comparable<Periode> {

	static final Periode[] DEFAULT_PERIODES;

	static {
		DEFAULT_PERIODES = new Periode[4 * 24]; // au quart-d'heure
		for (int i = 0; i < 24; ++i) {
			DEFAULT_PERIODES[4 * i] = new Periode(i, 0, i, 14);
			DEFAULT_PERIODES[4 * i + 1] = new Periode(i, 14, i, 29);
			DEFAULT_PERIODES[4 * i + 2] = new Periode(i, 30, i, 44);
			DEFAULT_PERIODES[4 * i + 3] = new Periode(i, 45, i, 59);
		}
	}

	private final HourMinutes start;
	private final HourMinutes end;

	Periode(int startHour, int startMinute, int endHour, int endMinute) {
		this.start = new HourMinutes(startHour, startMinute);
		this.end = new HourMinutes(endHour, endMinute);
	}

	public boolean isInPeriode(HourMinutes timespamp) {
		return start.compareTo(timespamp) <= 0 && timespamp.compareTo(end) <= 0;
	}

	public int compareTo(Periode o) {
		return Integer.valueOf(start.getHour()).compareTo(o.start.getHour());
	}

	@Override
	public String toString() {
		return String.format("%02d:%02d", start.getHour(), start.getMinutes());
	}
}
