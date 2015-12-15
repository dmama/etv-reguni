package ch.vd.uniregctb.migration.pm.log;

/**
 * Les différentes catégories (= formats de liste, donc a priori de listes différentes) des logs
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
	ASSUJETTISSEMENTS,

	/**
	 * Erreurs/messages liées aux données civiles migrées (ou abandonnées) de RegPM
	 */
	DONNEES_CIVILES_REGPM,

	/**
	 * Liste des rapports entre tiers générés (et accessoirement aussi de ceux qui ont finalement été abandonnés)
	 */
	RAPPORTS_ENTRE_TIERS,

	/**
	 * Liste des entreprises dont la forme juridique (au moment de la migration) est DP_APM
	 */
	DP_APM,

	/**
	 * Liste des fors principaux ignorés car ouverts postérieurement à toute date de fin d'assujettissement ICC
	 */
	FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT,

	/**
	 * Liste des fors fiscaux ignorés/non-repris/non-générés car aucun assujettissement ICC n'était présent dans RegPP
	 */
	FORS_IGNORES_AUCUN_ASSUJETTISSEMENT,

	/**
	 * Liste des entreprises qui ont un numéro IDE mais pas de numéro cantonal dans RegPM
	 */
	IDE_SANS_NO_CANTONAL,

	/**
	 * Liste des différences entre les raison sociale, forme juridique et numéro IDE entre les données de RegPM et les données issues de RCEnt
	 */
	DIFFERENCES_DONNEES_CIVILES

}
