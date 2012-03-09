package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc --> Longueur de colonne : 15 <!-- end-user-doc -->
 *
 * @author jec
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_y_mBoJOcEdy7DqR-SPIh9g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_y_mBoJOcEdy7DqR-SPIh9g"
 */
public enum TexteCasePostale {
	CASE_POSTALE("Case Postale %d", "Case Postale"),
	BOITE_POSTALE("Boîte Postale %d", "Boîte Postale"),
	POSTFACH("Postfach %d", "Postfach"),
	PO_BOX("PO Box %d", "PO Box"),
	CASELLA_POSTALE("Casella Postale %d", "Casella Postale");

	private final String formatWithNumber;
	private final String formatWithoutNumber;

	TexteCasePostale(String formatWithNumber, String formatWithoutNumber) {
		this.formatWithNumber = formatWithNumber;
		this.formatWithoutNumber = formatWithoutNumber;
	}

	/**
	 * @return la designation de la boîte postale sans le numéro.
	 */
	public String format() {
		return formatWithoutNumber;
	}

	/**
	 * Formatte le numéro de la case postale en fonction du type de réprésentation.
	 *
	 * @param numeroCasePostale le numéro à formatter
	 * @return l'adresse formattée
	 */
	public String format(int numeroCasePostale) {
		return String.format(formatWithNumber, numeroCasePostale);
	}

	/**
	 * Tente de trouver le type de case postale à partir du texte proposé,
	 *
	 * @param text un texte de boîte/case postale
	 * @return le type de case postale correspondante; ou CASE_POSTALE si aucun type ne correspond réellement.
	 */
	public static TexteCasePostale parse(String text) {
		for (TexteCasePostale t : values()) {
			if (t.name().equalsIgnoreCase(text)) {
				return t;
			}
		}
		// valeur par défaut
		return CASE_POSTALE;
	}
}
