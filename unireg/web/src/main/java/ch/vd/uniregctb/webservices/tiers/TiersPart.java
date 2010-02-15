/**
 *
 */
package ch.vd.uniregctb.webservices.tiers;

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
	 * Renseigne les adresses d'envoi (= six lignes pour l'adressage).
	 */
	ADRESSES_ENVOI,
	/**
	 * Renseigne les fors fiscaux.
	 */
	FORS_FISCAUX,
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
	 * Renseigne les situations de familles.
	 */
	SITUATIONS_FAMILLE,
	/**
	 * Renseigne les déclaration d'un tiers.
	 */
	DECLARATIONS;

	public static TiersPart fromValue(String v) {
		return valueOf(v);
	}
}
