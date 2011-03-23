package ch.vd.uniregctb.interfaces.model;


public enum LogicielMetier {
	ECH_99("ech-99"),
	EMPACI("empaci");

	private String format;

	LogicielMetier(String format) {
		this.format = format;
	}

	public String format() {
		return format;
	}

	public static LogicielMetier get(ch.vd.fidor.ws.v2.LogicielMetier right) {
		if (right == null) {
			return null;
		}

		switch (right) {
		case ECH_99:
			return LogicielMetier.ECH_99;
		case EMPACI:
			return LogicielMetier.EMPACI;
		default:
			throw new IllegalArgumentException("Valeur de logicielMetier non-supportée : " + right);

		}
	}
}
