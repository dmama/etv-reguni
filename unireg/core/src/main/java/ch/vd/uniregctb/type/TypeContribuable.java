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
	VAUDOIS_ORDINAIRE("ordinaire"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lNYS4DfHEd2EkOqealhanQ"
	 */
	VAUDOIS_DEPENSE("à la dépense"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nTylwDfHEd2EkOqealhanQ"
	 */
	HORS_CANTON("hors canton"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_pQEsQDfHEd2EkOqealhanQ"
	 */
	HORS_SUISSE("hors suisse"),
	/**
	 * Diplomate Suisse basé à l'étranger [UNIREG-1976]
	 */
	DIPLOMATE_SUISSE("diplomate suisse");

	private final String description;

	private TypeContribuable(String description) {
		this.description = description;
	}

	public String description() {
		return description;
	}
}
