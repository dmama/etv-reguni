package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Défini les parties d'un tiers renseignées de manière optionnelle par le web-service <i>tiers</i>.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>partyPartType</i> (xml) / <i>PartyPart</i> (client java)
 */
@XmlType(name = "TiersPart")
@XmlEnum(String.class)
public enum TiersPart {

	/**
	 * Renseigne les adresses courrier, poursuite, représentation et domicile.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ADDRESSES</i>.
	 */
	ADRESSES,
	/**
	 * Renseigne les adresses courrier, poursuite, représentation et domicile formattées pour l'envoi (= six lignes pour l'adressage).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>FORMATTED_ADDRESSES</i>.
	 */
	ADRESSES_ENVOI,
	/**
	 * Renseigne les fors fiscaux.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>TAX_RESIDENCES</i>.
	 */
	FORS_FISCAUX,
	/**
	 * Renseigne les fors fiscaux en ajoutant les fors fiscaux virtuels. C'est-à-dire les fors fiscaux qui n'existent pas en tant que tels,
	 * mais qui sont des vues construites en fonction de règles métier.
	 * <p>
	 * <b>Note:</b> son utilisation ajoute implicitement la partie {@link #FORS_FISCAUX}.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>VIRTUAL_TAX_RESIDENCES</i>.
	 *
	 * @see ForFiscal#virtuel
	 */
	FORS_FISCAUX_VIRTUELS,
	/**
	 * Renseigne les fors de gestion.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>MANAGING_TAX_RESIDENCES</i>.
	 */
	FORS_GESTION,
	/**
	 * Renseigne les personnes physiques principal et secondaire d'un ménage commun.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>HOUSEHOLD_MEMBERS</i>.
	 */
	COMPOSANTS_MENAGE,
	/**
	 * Renseigne les assujettissements d'un tiers.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ORDINARY_TAX_LIABILITIES</i> (ou <i>SIMPLIFIED_TAX_LIABILITIES</i>).
	 */
	ASSUJETTISSEMENTS,
	/**
	 * Renseigne les périodes d'imposition d'un tiers
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>TAXATION_PERIODS</i>.
	 */
	PERIODE_IMPOSITION,
	/**
	 * Renseigne les rapports entre tiers d'un tiers.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>RELATIONS_BETWEEN_PARTIES</i>.
	 */
	RAPPORTS_ENTRE_TIERS,
	/**
	 * Renseigne les situations de familles d'une personne physique ou d'un ménage-commun.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>FAMILY_STATUSES</i>.
	 */
	SITUATIONS_FAMILLE,
	/**
	 * Renseigne les déclaration d'un tiers.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>TAX_DECLARATIONS</i> (voir aussi <i>TAX_DECLARATIONS_STATUSES</i>).
	 */
	DECLARATIONS,
	/**
	 * Renseigne les comptes bancaires d'un tiers.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>BANK_ACCOUNTS</i>.
	 */
	COMPTES_BANCAIRES,
	/**
	 * Renseigne les sièges pour les personnes morales.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>LEGAL_SEATS</i>.
	 */
	SIEGES,
	/**
	 * Renseigne les formes juridiques pour les personnes morales.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>LEGAL_FORMS</i>.
	 */
	FORMES_JURIDIQUES,
	/**
	 * Renseigne les capitaux pour les personnes morales.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>CAPITALS</i>.
	 */
	CAPITAUX,
	/**
	 * Renseigne les régime fiscaux pour les personnes morales.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>TAX_SYSTEMS</i>.
	 */
	REGIMES_FISCAUX,
	/**
	 * Renseigne les états pour les personnes morales.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>CORPORATION_STATUSES</i>.
	 */
	ETATS_PM,
	/**
	 * Renseigne l'historique des périodicités pour le débiteur
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>DEBTOR_PERIODICITIES</i>.
	 */
	PERIODICITES;

	public static TiersPart fromValue(String v) {
		return valueOf(v);
	}
}
