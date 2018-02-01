package ch.vd.unireg.type;

/**
 * Différents types de lettre de bienvenue
 */
public enum TypeLettreBienvenue {

	/**
	 * Utilisé pour une entreprise de catégorie APM avec siège vaudois mais non-inscrite au RC
	 */
	APM_VD_NON_RC,

	/**
	 * Utilisé pour une entreprise vaudoise inscrite au RC
	 */
	VD_RC,

	/**
	 * Utilisé pour une entreprise non-vaudoise possédant un imeuble sur le sol cantonal
	 */
	HS_HC_IMMEUBLE,

	/**
	 * Utilisé pour une entreprise non-vaudoise possédant un établissement stable sur le sol cantonal (mais pas d'immeuble)
	 */
	HS_HC_ETABLISSEMENT
}
