package ch.vd.uniregctb.migration.pm.regpm;

public enum RegpmTypeAssujettissement {

	LILIC(1),
	LIFD(2),
	SANS(3);

	private final int id;

	private RegpmTypeAssujettissement(int id) {
		this.id = id;
	}

	public static RegpmTypeAssujettissement valueOf(int id) {
		for (RegpmTypeAssujettissement type : values()) {
			if (id == type.id) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown id " + id);
	}
}
