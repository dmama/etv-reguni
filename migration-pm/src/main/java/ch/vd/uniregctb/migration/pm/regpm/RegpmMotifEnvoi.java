package ch.vd.uniregctb.migration.pm.regpm;

public enum RegpmMotifEnvoi {

	FIN_EXER(1),
	FIN_ASSUJ(2),
	TRANS_SIEG(3);

	private final int id;

	private RegpmMotifEnvoi(int id) {
		this.id = id;
	}

	public static RegpmMotifEnvoi valueOf(int id) {
		for (RegpmMotifEnvoi type : values()) {
			if (id == type.id) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown id " + id);
	}

}
