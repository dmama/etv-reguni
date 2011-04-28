/**
 *
 */
package ch.vd.uniregctb.type;

/** 
 * <!-- begin-user-doc -->
 * Longueur de colonne : 15
 * <!-- end-user-doc -->
 * @author jec
 * 
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_y_mBoJOcEdy7DqR-SPIh9g"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_y_mBoJOcEdy7DqR-SPIh9g"
 */
public enum TexteCasePostale {
	/** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3K7ZoJOcEdy7DqR-SPIh9g"
	 */
	CASE_POSTALE("Case Postale %d"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LOYg4JOdEdy7DqR-SPIh9g"
	 */
	BOITE_POSTALE("Boîte Postale %d"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-dC8YJOcEdy7DqR-SPIh9g"
	 */
	POSTFACH("Postfach %d"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_QBpbkJOdEdy7DqR-SPIh9g"
	 */
	PO_BOX("PO Box %d"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zijIQJOdEdy7DqR-SPIh9g"
	 */
	CASELLA_POSTALE("Casella Postale %d");

	private final String format;

	TexteCasePostale(String format) {
		this.format = format;
	}

	/**
	 * Formatte le numéro de la case postale en fonction du type de réprésentation.
	 *
	 * @param numeroCasePostale
	 *            le numéro à formatter
	 * @return l'adresse formattée
	 */
	public String format(int numeroCasePostale) {
		return String.format(format, numeroCasePostale);
	}
}
