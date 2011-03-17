/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 12
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_U3Th0OqjEdyjCbp-wrQYpA"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_U3Th0OqjEdyjCbp-wrQYpA"
 */
public enum TypeEtatDeclaration {

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cvatkOqjEdyjCbp-wrQYpA"
	 */
	EMISE("émise"), /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fLIiEOqjEdyjCbp-wrQYpA"
	 */
	SOMMEE("sommée"), /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_yGKagO_6EdyJH9xfKrZiwA"
	 */
	ECHUE("échue"), /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_3nWyMO_6EdyJH9xfKrZiwA"
	 */
	RETOURNEE("retournée");

	private String description;

	TypeEtatDeclaration(String description) {
		this.description = description;
	}

	public String description() {
		return description;
	}

	/**
	 * Retourne le type d'etat du document correspondant à un code donné.
	 *
	 * @param code
	 *            le type d'etat du document
	 * @return le type d'etat du document correspondant à un code donné, null si le code n'a pas été trouvé.
	 */
	public static TypeEtatDeclaration valueOf(int code) {
		if (0 <= code && code < TypeEtatDeclaration.values().length) {
			return TypeEtatDeclaration.values()[code];
		}
		else {
			return null;
		}
	}
}
