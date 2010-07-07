/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 11
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7p5W0GHtEdydo47IZ53QMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7p5W0GHtEdydo47IZ53QMw"
 */
public enum ModeImposition {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_n8fF0Gg0Edy795uK-16JDw"
	 */
	ORDINAIRE("Ordinaire", "Imposition ordinaire"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uFR-cGHxEdydo47IZ53QMw"
	 */
	SOURCE("Source", "Imposition à la source"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Z4iuYHilEdyR8p78LDUQ5w"
	 */
	DEPENSE("Dépense", "Imposition d'après la dépense"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_cwlSgHilEdyR8p78LDUQ5w"
	 */
	MIXTE_137_1("Mixte 137 Al 1", "Imposition mixte"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_fYE5MHilEdyR8p78LDUQ5w"
	 */
	MIXTE_137_2("Mixte 137 Al 2", "Imposition mixte"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_hiEEgHilEdyR8p78LDUQ5w"
	 */
	INDIGENT("Indigent", "Imposition ordinaire");

	private String texte;

	private String texteEnrichi;

	private ModeImposition(String format, String formatEnrichi) {
		this.texte = format;
		this.texteEnrichi = formatEnrichi;
	}

	public String texte() {
		return texte;
	}

	public String texteEnrichi() {
		return texteEnrichi;
	}

	public boolean isAuRole() {
		return this != SOURCE;
	}
}
