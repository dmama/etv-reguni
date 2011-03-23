/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 17
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_coHGMDfHEd2EkOqealhanQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_coHGMDfHEd2EkOqealhanQ"
 */
public enum TypeContribuable {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fE4DkDfHEd2EkOqealhanQ"
	 */
	VAUDOIS_ORDINAIRE("ordinaire", "OR"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lNYS4DfHEd2EkOqealhanQ"
	 */
	VAUDOIS_DEPENSE("à la dépense", "DE"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nTylwDfHEd2EkOqealhanQ"
	 */
	HORS_CANTON("hors canton", "HC"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pQEsQDfHEd2EkOqealhanQ"
	 */
	HORS_SUISSE("hors suisse", "HS"),
	/**
	 * Diplomate Suisse basé à l'étranger [UNIREG-1976]
	 */
	DIPLOMATE_SUISSE("diplomate suisse", "DS");

	private String description;

	private String descriptionAcomptes;

	private TypeContribuable(String description, String descriptionAcomptes) {
		this.description = description;
		this.descriptionAcomptes = descriptionAcomptes;
	}

	public String description() {
		return description;
	}

	public String descriptionAcomptes() {
		return descriptionAcomptes;
	}
}
