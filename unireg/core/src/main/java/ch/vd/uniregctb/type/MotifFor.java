package ch.vd.uniregctb.type;

public enum MotifFor {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_7pc54PY1Edyw0I40oDFBsg"
	 */
	DEMENAGEMENT_VD("Déménagement"), /**
	 * <!-- begin-user-doc -->
	 * correspond a VEUVAGE_DISSOLUTION_PARTENARIAT_DECES
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_PtEZMPY2Edyw0I40oDFBsg"
	 */
	VEUVAGE_DECES("Veuvage", "Décès"), /**
	 * <!-- begin-user-doc -->
	 * correspond a MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_MHKpgPY2Edyw0I40oDFBsg"
	 */
	MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION("Mariage / enregistrement partenariat / réconciliation"), /**
	 * <!-- begin-user-doc -->
	 * correspond a DIVORCE_DISSOLUTION_PARTENARIAT_SEPARATION
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_JMK4APY2Edyw0I40oDFBsg"
	 */
	SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT("Séparation / divorce / dissolution partenariat"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Cz68gPY2Edyw0I40oDFBsg"
	 */
	PERMIS_C_SUISSE("Obtention de permis C / nationalité suisse"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BHTCoPY2Edyw0I40oDFBsg"
	 */
	MAJORITE("Majorité"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_536q0PY1Edyw0I40oDFBsg"
	 */
	ARRIVEE_HS("Arrivée de hors-Suisse"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_02-ggPY1Edyw0I40oDFBsg"
	 */
	ARRIVEE_HC("Arrivée de hors-canton"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lzSyMBFNEd2znZ2YYJRJkQ"
	 */
	FUSION_COMMUNES("Fusion de communes"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lR3cMGO3Ed2P557iT3FnjQ"
	 */
	ACHAT_IMMOBILIER("Achat immobilier"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_24tXEGO3Ed2P557iT3FnjQ"
	 */
	VENTE_IMMOBILIER("Vente immobilier"),/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_r732EGO3Ed2P557iT3FnjQ"
	 */
	DEBUT_EXPLOITATION("Début d'exploitation"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_532owGO3Ed2P557iT3FnjQ"
	 */
	FIN_EXPLOITATION("Fin d'exploitation"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_V7x-MPY2Edyw0I40oDFBsg"
	 */
	DEPART_HS("Départ hors-Suisse"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_T53JwPY2Edyw0I40oDFBsg"
	 */
	DEPART_HC("Départ hors-canton"),/**
	* <!-- begin-user-doc -->
	 * Interdit d'utilisation dans tous les cas sauf pour la migration des données depuis le Host.
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_r732EGO3Ed2P557iT3FnjQ"
	 */
	@Deprecated
	INDETERMINE("Indéterminé"),/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_T53JwPY2Edyw0I40oDFBsg"
	 */
	SEJOUR_SAISONNIER("Début de séjour saisonnier", "Fin de séjour saisonnier"),
	/**
	 * Utilisé lors du changement du mode d'imposition,
	 */
	CHGT_MODE_IMPOSITION("Changement du mode d'imposition"),

	/**
	 * Annulation
	 */
	ANNULATION("Annulation"),

	/**
	 * Réactivation
	 */
	REACTIVATION("Réactivation"),
	/**
	 * Début d'activité diplomatique d'un diplomatique suisse basé à l'étranger ([UNIREG-911])
	 */
	DEBUT_ACTIVITE_DIPLOMATIQUE("Début d'activité diplomatique"),
	/**
	 * Début d'activité diplomatique d'un diplomatique suisse basé à l'étranger ([UNIREG-911])
	 */
	FIN_ACTIVITE_DIPLOMATIQUE("Fin d'activité diplomatique");

	private final String descriptionOuverture;

	private final String descriptionFermeture;

	private MotifFor(String descriptionOuverture, String descriptionFermeture) {
		this.descriptionOuverture = descriptionOuverture;
		this.descriptionFermeture = descriptionFermeture;
	}

	private MotifFor(String description) {
		this(description, description);
	}

	public String getDescription(boolean ouverture) {
		return ouverture ? descriptionOuverture : descriptionFermeture;
	}
}
