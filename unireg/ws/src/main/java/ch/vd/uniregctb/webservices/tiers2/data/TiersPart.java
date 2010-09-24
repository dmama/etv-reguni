/**
 *
 */
package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Défini les parties d'un tiers renseignées de manière optionnelle par le web-service <i>tiers</i>.
 */
@XmlType(name = "TiersPart")
@XmlEnum(String.class)
public enum TiersPart {

	/**
	 * Renseigne les adresses courrier, poursuite, représentation et domicile.
	 */
	ADRESSES,
	/**
	 * Renseigne les adresses courrier, poursuite, représentation et domicile formattées pour l'envoi (= six lignes pour l'adressage).
	 */
	ADRESSES_ENVOI,
	/**
	 * Renseigne les fors fiscaux.
	 */
	FORS_FISCAUX,
	/**
	 * Renseigne les fors fiscaux en ajoutant les fors fiscaux virtuels. C'est-à-dire les fors fiscaux qui n'existent pas en tant que tels,
	 * mais qui sont des vues construites en fonction de règles métier.
	 * <p>
	 * <b>Note:</b> son utilisation ajoute implicitement la partie {@link #FORS_FISCAUX}.
	 *
	 * @see ForFiscal#virtuel
	 */
	FORS_FISCAUX_VIRTUELS,
	/**
	 * Renseigne les fors de gestion.
	 */
	FORS_GESTION,
	/**
	 * Renseigne les personnes physiques principal et secondaire d'un ménage commun.
	 */
	COMPOSANTS_MENAGE,
	/**
	 * Renseigne les assujettissements d'un tiers.
	 */
	ASSUJETTISSEMENTS,
	/**
	 * Renseigne les périodes d'imposition d'un tiers
	 */
	PERIODE_IMPOSITION,
	/**
	 * Renseigne les rapports entre tiers d'un tiers.
	 */
	RAPPORTS_ENTRE_TIERS,
	/**
	 * Renseigne les situations de familles d'une personne physique ou d'un ménage-commun.
	 */
	SITUATIONS_FAMILLE,
	/**
	 * Renseigne les déclaration d'un tiers.
	 */
	DECLARATIONS,
	/**
	 * Renseigne les comptes bancaires d'un tiers.
	 */
	COMPTES_BANCAIRES,
	/**
	 * Renseigne les sièges pour les personnes morales.
	 */
	SIEGES,
	/**
	 * Renseigne les formes juridiques pour les personnes morales.
	 */
	FORMES_JURIDIQUES,
	/**
	 * Renseigne les capitaux pour les personnes morales.
	 */
	CAPITAUX,
	/**
	 * Renseigne les régime fiscaux pour les personnes morales.
	 */
	REGIMES_FISCAUX,
	/**
	 * Renseigne les états pour les personnes morales.
	 */
	ETATS_PM,
	/**
	 * Renseigne l'historique des périodicités pour le débiteur
	 */
	PERIODICITES;

	public static TiersPart fromValue(String v) {
		return valueOf(v);
	}
}
