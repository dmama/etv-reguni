/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 35
 * <!-- end-user-doc -->
 * Genre de permis réglementant le séjour d'une personne étrangère en Suisse.
 * Voir eCH-0006 pour les valeurs possibles
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IOZZMGHuEdydo47IZ53QMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IOZZMGHuEdydo47IZ53QMw"
 */
public enum CategorieEtranger {
	_01_SAISONNIER_A,
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Thw9EHimEdyR8p78LDUQ5w"
	 */
	_02_PERMIS_SEJOUR_B, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Up5CwHimEdyR8p78LDUQ5w"
	 */
	_03_ETABLI_C, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_VTKpQHimEdyR8p78LDUQ5w"
	 */
	_04_CONJOINT_DIPLOMATE_CI, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_V98EkHimEdyR8p78LDUQ5w"
	 */
	_05_ETRANGER_ADMIS_PROVISOIREMENT_F, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Zq4QYHimEdyR8p78LDUQ5w"
	 */
	_06_FRONTALIER_G, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_aiTnoHimEdyR8p78LDUQ5w"
	 */
	_07_PERMIS_SEJOUR_COURTE_DUREE_L, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_bO3LoHimEdyR8p78LDUQ5w"
	 */
	_08_REQUERANT_ASILE_N, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_b-stIHimEdyR8p78LDUQ5w"
	 */
	_09_A_PROTEGER_S, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1lmO0HimEdyR8p78LDUQ5w"
	 */
	_10_TENUE_DE_S_ANNONCER,
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_My6nIPY0Edyw0I40oDFBsg"
	 */
	_11_DIPLOMATE,
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2zFVoHimEdyR8p78LDUQ5w"
	 */
	_12_FONCTIONNAIRE_INTERNATIONAL, /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3bdkQHimEdyR8p78LDUQ5w"
	 */
	_13_NON_ATTRIBUEE;
	
	public static CategorieEtranger enumToCategorie(TypePermis permis) {
		if (permis == null)
			return null;
		
		if (TypePermis.ANNUEL == permis) {
			return _02_PERMIS_SEJOUR_B;
		}
		else if (TypePermis.ETABLISSEMENT == permis) {
			return _03_ETABLI_C;
		}
		else if (TypePermis.FRONTALIER == permis) {
			return _06_FRONTALIER_G;
		}
		else if (TypePermis.SUISSE_SOURCIER == permis) {
			return null;
		}
		else if (TypePermis.PROVISOIRE == permis) {
			return _05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		}
		else if (TypePermis.REQUERANT_ASILE_AVANT_DECISION == permis) {
			return _08_REQUERANT_ASILE_N;
		}
		else if (TypePermis.REQUERANT_ASILE_REFUSE == permis) {
			return _05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		}
		else if (TypePermis.COURTE_DUREE == permis) {
			return _07_PERMIS_SEJOUR_COURTE_DUREE_L;
		}
		else if (TypePermis.DIPLOMATE == permis) {
			return _11_DIPLOMATE;
		}
		else if (TypePermis.FONCTIONNAIRE_INTERNATIONAL == permis) {
			return _12_FONCTIONNAIRE_INTERNATIONAL;
		}
		else if (TypePermis.PERSONNE_A_PROTEGER == permis) {
			return _09_A_PROTEGER_S;
		}
		else {
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}
}