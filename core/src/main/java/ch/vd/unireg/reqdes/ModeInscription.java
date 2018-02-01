package ch.vd.unireg.reqdes;

/**
 * Mode d'inscription d'une transaction immobilière dans un acte désigné
 * <p/>
 * Longueur de colonne : 12
 */
public enum ModeInscription {
	INSCRIPTION("Inscription"),
	MODIFICATION("Modification"),
	RADIATION("Radiation");

	private final String displayName;

	ModeInscription(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
