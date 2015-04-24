package ch.vd.uniregctb.migration.pm.regpm;

public enum RegpmTypeAdresseIndividu {

	COURRIER("C"),
	PRINCIPALE("P"),
	SECONDAIRE("S");

	private final String code;

	private RegpmTypeAdresseIndividu(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static RegpmTypeAdresseIndividu byCode(String code) {
		for (RegpmTypeAdresseIndividu a : values()) {
			if (a.code.equals(code)) {
				return a;
			}
		}
		throw new IllegalArgumentException("Code de type d'adresse inconnu : " + code);
	}
}
