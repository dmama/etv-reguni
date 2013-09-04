package ch.vd.uniregctb.type;

public enum MotifFor {

	DEMENAGEMENT_VD("Déménagement"),

	VEUVAGE_DECES("Veuvage", "Décès"),

	MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION("Mariage / enregistrement partenariat / réconciliation"),

	SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT("Séparation / divorce / dissolution partenariat"),

	PERMIS_C_SUISSE("Obtention de permis C / nationalité suisse"),

	MAJORITE("Majorité"),

	ARRIVEE_HS("Arrivée de hors-Suisse"),

	ARRIVEE_HC("Arrivée de hors-canton"),

	FUSION_COMMUNES("Fusion de communes"),

	ACHAT_IMMOBILIER("Achat immobilier"),

	VENTE_IMMOBILIER("Vente immobilier"),

	DEBUT_EXPLOITATION("Début d'exploitation"),

	FIN_EXPLOITATION("Fin d'exploitation"),

	DEPART_HS("Départ hors-Suisse"),

	DEPART_HC("Départ hors-canton"),

	@Deprecated
	INDETERMINE("Indéterminé"),

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
	FIN_ACTIVITE_DIPLOMATIQUE("Fin d'activité diplomatique"),

	/**
	 * Début d'activité d'un débiteur IS (date de début correspondante toujours un premier de mois)
	 */
	DEBUT_PRESTATION_IS("Début de prestation IS"),

	/**
	 * Fin d'activité d'un débiteur IS (date de fin correspondante toujours en fin d'année)
	 */
	FIN_PRESTATION_IS("Fin de prestation IS"),

	/**
	 * Cessation d'activité / fusion / faillite d'un débiteur de prestations imposables (date de fin correspondante toujours en fin de mois)
	 */
	CESSATION_ACTIVITE_FUSION_FAILLITE("Cessation d'activité / fusion / faillite");

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
