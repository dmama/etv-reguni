package ch.vd.uniregctb.migration.pm.regpm;

public enum RegpmTypeAdresseEntreprise {

	COURRIER("C"),
	FACTURATION("F"),
	SIEGE("S");

	private final String code;

	private RegpmTypeAdresseEntreprise(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static RegpmTypeAdresseEntreprise byCode(String code) {
		for (RegpmTypeAdresseEntreprise a : values()) {
			if (a.code.equals(code)) {
				return a;
			}
		}
		throw new IllegalArgumentException("Code de type d'adresse inconnu : " + code);
	}
}
