package ch.vd.uniregctb.webservices.tiers2.stats;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.utils.Assert;

class HourMinutes implements Comparable<HourMinutes> {
	private final int hour;
	private final int minutes;

	public HourMinutes(int hour, int minutes) {
		Assert.isTrue(0 <= hour && hour < 24);
		Assert.isTrue(0 <= minutes && minutes < 60);
		this.hour = hour;
		this.minutes = minutes;
	}

	// exemple : "10:18"

	public static HourMinutes parse(String string) {
		if (StringUtils.isBlank(string)) {
			return null;
		}

		int hour = Integer.parseInt(string.substring(0, 2));
		int minutes = Integer.parseInt(string.substring(3, 5));
		return new HourMinutes(hour, minutes);
	}

	public int getHour() {
		return hour;
	}

	public int getMinutes() {
		return minutes;
	}

	public int compareTo(HourMinutes o) {
		if (this.hour < o.hour) {
			return -1;
		}
		else if (this.hour > o.hour) {
			return 1;
		}
		if (this.minutes < o.minutes) {
			return -1;
		}
		else if (this.minutes > o.minutes) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
