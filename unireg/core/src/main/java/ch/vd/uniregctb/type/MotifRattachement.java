/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 26
 * <!-- end-user-doc -->
 * Valeurs permises :
 * - Domicile ou séjour
 * - Propriété d'immeuble
 * - Propriété d'entreprise
 * - Exploitation d'un établissement stable
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_W8C5YGHuEdydo47IZ53QMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_W8C5YGHuEdydo47IZ53QMw"
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