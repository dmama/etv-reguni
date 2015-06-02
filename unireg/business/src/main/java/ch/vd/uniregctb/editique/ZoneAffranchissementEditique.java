package ch.vd.uniregctb.editique;

public enum ZoneAffranchissementEditique {
	SUISSE("CH"),
	EUROPE("EU"),
	RESTE_MONDE("RM"),
	INCONNU("NA");

	private final String code;

	private ZoneAffranchissementEditique(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
