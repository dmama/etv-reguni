/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_OkeKcGHuEdydo47IZ53QMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_OkeKcGHuEdydo47IZ53QMw"
 */
public enum FormulePolitesse {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_zl3n4EmNEd2R_dBVHKOj6Q"
	 */
	MADAME_MONSIEUR("Madame, Monsieur"),
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DaNxIGHxEdydo47IZ53QMw"
	 */
	MONSIEUR("Monsieur"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_CO7CsGHxEdydo47IZ53QMw"
	 */
	MADAME("Madame"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DIkAcGHxEdydo47IZ53QMw"
	 */
	MESSIEURS("Messieurs"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_C8J8IGHxEdydo47IZ53QMw"
	 */
	MESDAMES("Mesdames"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Cvk4sGHxEdydo47IZ53QMw"
	 */
	MONSIEUR_ET_MADAME("Monsieur et Madame"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_8LuDsHilEdyR8p78LDUQ5w"
	 */
	HERITIERS("Aux héritiers de") {
		/**
		 * [UNIREG-1398] la formule d'appel dans ce cas est "Madame, Monsieur".
		 */
		@Override
		public String formuleAppel() {
			return MADAME_MONSIEUR.salutations();
		}
	},

	/**
	 * [UNIREG-2302] Formule de politesse à l'usage des personnes morales.
	 */
	PERSONNE_MORALE(null) {
		@Override
		public String formuleAppel() {
			return MADAME_MONSIEUR.salutations();
		}
	};

	private String format;

	FormulePolitesse(String format) {
		this.format = format;
	}

	/**
	 * @return les salutations selon les us et coutumes de l'ACI. Exemples :
	 *         <ul>
	 *         <li>Monsieur</li>
	 *         <li>Madame</li>
	 *         <li>Aux héritiers de</li>
	 *         <li>...</li>
	 *         </ul>
	 */
	public String salutations() {
		return format;
	}

	/**
	 * [UNIREG-1398]
	 *
	 * @return la formule d'appel stricte. C'est-à-dire les salutations mais <b>sans formule spéciale</b> propre à l'ACI (pas de <i>Aux
	 *         héritiers de</i>). Exemples :
	 *         <ul>
	 *         <li>Monsieur</li>
	 *         <li>Madame</li>
	 *         <li>Madame, Monsieur</li>
	 *         <li>...</li>
	 *         </ul>
	 */
	public String formuleAppel() {
		return format;
	}
}