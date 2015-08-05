package ch.vd.uniregctb.migration.pm.log;

/**
 * Les différentes catégories (= formats de liste, donc a priori de listes différentes) des logs
 * TODO il y en a certainement d'autres encore
 */
public enum LogCategory {

	/**
	 * Informations générales (en général erreurs...) qui ne sont liées à aucune entité clairement identifiable...
	 */
	EXCEPTIONS,

	/**
	 * Informations générales qui n'ont pas vraiment leur place ailleurs (exceptions...) mais sont liées à une entité particulières
	 */
	SUIVI,

	/**
	 * Erreurs/messages liés à la migration des adresses
	 */
	ADRESSES,

	/**
	 * Erreurs/messages liés à la migration des individus
	 */
	INDIVIDUS_PM,

	/**
	 * Erreurs/messages liés à la migration des établissements (secondaires)
	 */
	ETABLISSEMENTS,

	/**
	 * Erreurs/messages liés à la migration des fors fiscaux
	 */
	FORS,

	/**
	 * Erreurs/messages liés à la migration des déclarations
	 */
	DECLARATIONS,

	/**
	 * Erreurs/messages liés à la migration des coordonnées financières
	 */
	COORDONNEES_FINANCIERES,

	/**
	 * Erreurs/messages liés aux assujettissements
	 */
	ASSUJETTISSEMENTS

}
