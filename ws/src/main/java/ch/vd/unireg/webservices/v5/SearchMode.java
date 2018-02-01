package ch.vd.unireg.webservices.v5;

public enum SearchMode {

	/**
	 * Ne retourne que les tiers dont les mots du nom correspondent exactement aux mots spécifiés (par exemple: 'Dujardin' dans 'Jean Dujardin').
	 * 						A noter que l'ordre des mots et la présence éventuelle de mots supplémentaires n'ont aucune importance. Traitement plutôt rapide.
	 *
	 *
	 */
	IS_EXACTLY,

	/**
	 * Retourne les tiers dont le nom contient la valeur spécifiée (par exemple: 'jardin' dans 'Jean Dujardin'). Traitement moyennement rapide.
	 *
	 *
	 */
	CONTAINS,

	/**
	 * Retourne les tiers dont le nom est assez proche de la valeur spécifiée (par exemple: 'xardin' dans 'Jean Dujardin'). Traitement le moins rapide.
	 *
	 *
	 */
	PHONETIC
}
