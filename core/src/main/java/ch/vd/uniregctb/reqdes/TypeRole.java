package ch.vd.uniregctb.reqdes;

/**
 * Type énuméré des différent rôles que peut prendre une partie prenante dans un acte authentique
 * <p/>
 * Longueur de colonne : 10
 */
public enum TypeRole {
	ACQUEREUR("Acquéreur"),
	ALIENATEUR("Aliénateur"),
	AUTRE("Autre");

	private final String displayName;

	TypeRole(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
