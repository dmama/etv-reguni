package ch.vd.unireg.interfaces.infra.data;


import org.apache.commons.lang3.StringUtils;

public enum LogicielMetier {
	ECH_99("ech-99"),
	EMPACI("empaci");

	private final String format;

	LogicielMetier(String format) {
		this.format = format;
	}

	public String format() {
		return format;
	}

	public static LogicielMetier get(String right) {
		if (StringUtils.isBlank(right)) {
			return null;
		}

		if (ECH_99.name().equalsIgnoreCase(right)) {
			return LogicielMetier.ECH_99;
		}
		else if (EMPACI.name().equalsIgnoreCase(right)) {
			return LogicielMetier.EMPACI;
		}
		else {
			throw new IllegalArgumentException("Valeur de logicielMetier non-supportée : " + right);
		}
	}
}
