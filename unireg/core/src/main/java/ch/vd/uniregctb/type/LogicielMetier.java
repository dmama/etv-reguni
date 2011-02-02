package ch.vd.uniregctb.type;

public enum LogicielMetier {
	ECH_99 ("ech-99"),
	EMPACI ("empaci");

	private String format;

	LogicielMetier(String format) {
		this.format = format;
	}

	public String format() {
		return format;
	}
}
