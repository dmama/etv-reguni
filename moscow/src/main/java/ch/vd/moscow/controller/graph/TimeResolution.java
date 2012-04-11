package ch.vd.moscow.controller.graph;

public enum TimeResolution {
	MINUTE(1),
	FIVE_MINUTES(5),
	FIFTEEN_MINUTES(15),
	HOUR(60),
	DAY(1440);

	private int minutes;

	TimeResolution(int minutes) {
		this.minutes = minutes;
	}

	public int getMinutes() {
		return minutes;
	}
}
