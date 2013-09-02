/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc --> Longueur de colonne : 29 <!-- end-user-doc -->
 *
 * @author jec
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_EM7REC4AEd2H4bonmeBdag"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_EM7REC4AEd2H4bonmeBdag"
 */
public enum TypeDocument {
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_bJc3MC4AEd2H4bonmeBdag"
	 */
	DECLARATION_IMPOT_COMPLETE_BATCH {
		@Override
		public boolean isOrdinaire() {
			return true;
		}

		@Override
		public String getDescription() {
			return "déclaration d'impôt ordinaire complète";
		}
	},

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_bJc3MC4AEd2H4bonmeBdag"
	 */
	DECLARATION_IMPOT_COMPLETE_LOCAL {
		@Override
		public boolean isOrdinaire() {
			return true;
		}

		@Override
		public String getDescription() {
			return "déclaration d'impôt ordinaire complète";
		}
	},

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_grXAkC4AEd2H4bonmeBdag"
	 */
	DECLARATION_IMPOT_VAUDTAX {
		@Override
		public boolean isOrdinaire() {
			return true;
		}

		@Override
		public String getDescription() {
			return "déclaration d'impôt ordinaire VaudTax";
		}
	},

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lVZGwC4AEd2H4bonmeBdag"
	 */
	DECLARATION_IMPOT_DEPENSE {
		@Override
		public boolean isOrdinaire() {
			return false;
		}

		@Override
		public String getDescription() {
			return "déclaration d'impôt dépense";
		}
	},

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_qAwpsC4AEd2H4bonmeBdag"
	 */
	DECLARATION_IMPOT_HC_IMMEUBLE {
		@Override
		public boolean isOrdinaire() {
			return false;
		}

		@Override
		public String getDescription() {
			return "déclaration d'impôt hors-canton immeuble";
		}
	},

	LISTE_RECAPITULATIVE {
		@Override
		public boolean isOrdinaire() {
			return false;
		}

		@Override
		public String getDescription() {
			return "liste récapitulative";
		}
	},

	E_FACTURE_ATTENTE_CONTACT{
		@Override
		public boolean isOrdinaire() {
			return false;
		}

		@Override
		public String getDescription() {
			return "Document de demande de contact pour E-facture";
		}
	},

	E_FACTURE_ATTENTE_SIGNATURE{
		@Override
		public boolean isOrdinaire() {
			return false;
		}

		@Override
		public String getDescription() {
			return "Document de demande de signature pour confirmation E-facture";
		}
	};

	/**
	 * @return <b>vrai</b> si le type de document correspond à une déclaration ordinaire (complète manuelle, complète batch ou vaudtax).
	 */
	public abstract boolean isOrdinaire();

	/**
	 * @return une description textuelle du type de document
	 */
	public abstract String getDescription();
}