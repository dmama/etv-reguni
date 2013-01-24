/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * <!-- begin-user-doc -->
 * Longueur de colonne : 22
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
	ETABLISSEMENT_STABLE
}