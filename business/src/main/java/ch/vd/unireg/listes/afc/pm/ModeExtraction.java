package ch.vd.unireg.listes.afc.pm;

/**
 * Modes d'extraction des données RPT PM
 */
public enum ModeExtraction {

	BENEFICE("Bénéfice");

	private final String description;

	ModeExtraction(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
