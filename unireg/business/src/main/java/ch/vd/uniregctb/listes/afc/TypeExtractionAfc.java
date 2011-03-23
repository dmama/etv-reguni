package ch.vd.uniregctb.listes.afc;

/**
 * Type de l'extraction AFC à générer : REVENU ou FORTUNE
 */
public enum TypeExtractionAfc {

	REVENU("Revenu"),
	FORTUNE("Fortune");

	private final String description;

	private TypeExtractionAfc(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
