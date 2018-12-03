package ch.vd.unireg.type;

import java.util.Arrays;
import java.util.List;

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

	DEBUT_EXPLOITATION("Début d'activité"),

	FIN_EXPLOITATION("Fin d'activité"),

	DEPART_HS("Départ hors-Suisse"),

	DEPART_HC("Départ hors-canton"),

	/**
	 * Motif indéterminé. Valeur utilisée pour les fors migrés, à ne pas utiliser pour de nouveau fors.
	 */
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
	 * Début d'activité d'un débiteur IS
	 */
	DEBUT_PRESTATION_IS("Début de prestation IS"),

	/**
	 * Fin d'activité d'un débiteur IS
	 */
	FIN_PRESTATION_IS("Fin de prestation IS"),

	/**
	 * Cessation d'activité / fusion / faillite d'un débiteur IS
	 */
	CESSATION_ACTIVITE_FUSION_FAILLITE("Cessation d'activité / fusion / faillite"),

	/**
	 * Déménagement du siège social d'un débiteur IS
	 */
	DEMENAGEMENT_SIEGE("Déménagement de siège"),

	/**
	 * Fusion d'entreprises
	 */
	FUSION_ENTREPRISES("Fusion d'entreprises"),

	/**
	 * Faillite d'une entreprise
	 */
	FAILLITE("Faillite");

	/**
	 * Liste des motifs d'ouverture qui represente un début d'activité.
	 */
	final private static List<MotifFor> MOTIFS_DEBUT_ACTIVITE = Arrays.asList(ARRIVEE_HS, ARRIVEE_HC, FUSION_ENTREPRISES, DEBUT_EXPLOITATION);

	private final String descriptionOuverture;

	private final String descriptionFermeture;

	MotifFor(String descriptionOuverture, String descriptionFermeture) {
		this.descriptionOuverture = descriptionOuverture;
		this.descriptionFermeture = descriptionFermeture;
	}

	MotifFor(String description) {
		this(description, description);
	}

	public String getDescription(boolean ouverture) {
		return ouverture ? descriptionOuverture : descriptionFermeture;
	}

	/**
	 * Liste des motifs d'ouverture qui represente un début d'activité.
	 *
	 * @return Liste des motifs d'ouverture qui represente un début d'activité
	 */
	public static List<MotifFor> getListeMotifsDebutActiviteCommerciale() {
		return MOTIFS_DEBUT_ACTIVITE;
	}
}
