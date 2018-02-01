package ch.vd.unireg.type;

public enum Sexe {
	FEMININ("Féminin"),
	MASCULIN("Masculin");

	private final String displayName;

	Sexe(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}