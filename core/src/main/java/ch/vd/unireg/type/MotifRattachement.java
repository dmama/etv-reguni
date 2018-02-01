package ch.vd.uniregctb.type;

/**
 * Longueur de colonne : 26
 */
public enum MotifRattachement {
	DOMICILE,
	IMMEUBLE_PRIVE,
	DIPLOMATE_SUISSE,
	ACTIVITE_INDEPENDANTE,
	SEJOUR_SAISONNIER,
	DIRIGEANT_SOCIETE,
	ACTIVITE_LUCRATIVE_CAS,
	ADMINISTRATEUR,
	CREANCIER_HYPOTHECAIRE,
	PRESTATION_PREVOYANCE,
	DIPLOMATE_ETRANGER,
	LOI_TRAVAIL_AU_NOIR,
	/**
	 * Utilisé pour les fors secondaires des PMs
	 */
	ETABLISSEMENT_STABLE,

	/**
	 * Utilisé pour les fors secondaires "source" des bénéficiaires de participations hors-Suisse
	 * @since 5.8
	 */
	PARTICIPATIONS_HORS_SUISSE,

	/**
	 * Utilisé pour les fors secondaires "source" des ouvriers agricoles viticoles (<i>aka</i> effeuilleuses)
	 * @since 5.8
	 */
	EFFEUILLEUSES
}