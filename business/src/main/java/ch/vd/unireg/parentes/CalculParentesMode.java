package ch.vd.uniregctb.parentes;

/**
 * Mode du job de recalcul des relations de parenté
 */
public enum CalculParentesMode {

	/**
	 * Ré-initialisation complete
	 */
	FULL,

	/**
	 * Rafraîchissement de toute la population concernée (= personnes physiques habitantes)
	 */
	REFRESH_ALL,

	/**
	 * Rafraîchissement de la seule population "dirty"
	 */
	REFRESH_DIRTY
}
