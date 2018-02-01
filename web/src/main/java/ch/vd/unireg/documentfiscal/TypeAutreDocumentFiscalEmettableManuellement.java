package ch.vd.unireg.documentfiscal;

/**
 * Types des différents "autres documents fiscaux" émettables manuellement
 * depuis l'IHM (documents sans suivi...)
 */
public enum TypeAutreDocumentFiscalEmettableManuellement {

	/**
	 * Lettre d'autorisation de radiation d'une entreprise au RC
	 */
	AUTORISATION_RADIATION,

	/**
	 * Demande de bilan final à une entreprise suite à une demande de radiation du RC
	 */
	DEMANDE_BILAN_FINAL,

	/**
	 * Demande d'informations complémentaire au sujet de la liquidation d'une entreprise
	 */
	LETTRE_TYPE_INFORMATION_LIQUIDATION,

	/**
	 * Lettre de bienvenue.
	 */
	LETTRE_BIENVENUE
}
