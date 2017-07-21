package ch.vd.uniregctb.listes.afc;

/**
 * Type de l'extraction des données de référence RPT à générer :
 * {@link #REVENU_ORDINAIRE revenu rôle ordinaire}, {@link #REVENU_SOURCE_PURE revenu source pure} ou {@link #FORTUNE fortune}
 */
public enum TypeExtractionDonneesRpt {

	REVENU_ORDINAIRE("Revenu rôle ordinaire"),
	REVENU_SOURCE_PURE("Revenu source pure"),
	FORTUNE("Fortune"),
	IBC("Impot bénéfice capital");

	private final String description;

	TypeExtractionDonneesRpt(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
