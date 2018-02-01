package ch.vd.uniregctb.listes.afc.pm;

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
