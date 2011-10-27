/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 34
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_n8TIUNYAEdyUwLf8TSgq3w"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_n8TIUNYAEdyUwLf8TSgq3w"
 */
public enum EtatCivil {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_qPq5INYAEdyUwLf8TSgq3w"
	 */
	CELIBATAIRE ("Célibataire"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vCyp4NYAEdyUwLf8TSgq3w"
	 */
	MARIE ("Marié(e)"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0TtJ4NYAEdyUwLf8TSgq3w"
	 */
	VEUF ("Veuf(ve)"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_5maZoNYAEdyUwLf8TSgq3w"
	 */
	LIE_PARTENARIAT_ENREGISTRE ("Lié(e) partenariat enregistré"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_8rabsNYAEdyUwLf8TSgq3w"
	 */
	NON_MARIE ("Non marié(e)"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_eanhQNYBEdyUwLf8TSgq3w"
	 */
	PARTENARIAT_DISSOUS_JUDICIAIREMENT ("Partenariat dissous judiciairement"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_odAzQNYBEdyUwLf8TSgq3w"
	 */
	DIVORCE ("Divorcé(e)"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lIvbQCWDEd20nJwltNbIIQ"
	 */
	SEPARE ("Séparé(e)"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1x9h4C43Ed2AVO1NDG9Pqw"
	 */
	PARTENARIAT_DISSOUS_DECES ("Partenariat dissous décès");

	private final String format;

	EtatCivil(String format) {
		this.format = format;
	}

	public String format() {
		return format;
	}
}
