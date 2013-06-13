package ch.vd.uniregctb.webservices.tiers2.impl;

/**
 * Mode de copie des parts d'un Tiers.
 */
public enum CopyMode {
	/**
	 * Dans ce mode, les données copiées sont ajoutées aux données présentes
	 */
	ADDITIF,
	/**
	 * Dans ce mode, les données copiées remplacent les données présentes
	 */
	EXCLUSIF
}
