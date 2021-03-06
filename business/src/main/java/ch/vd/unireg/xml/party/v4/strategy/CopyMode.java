package ch.vd.unireg.xml.party.v4.strategy;

/**
 * Mode de copie des parts d'un Tiers.
 */
public enum CopyMode {
	/**
	 * Dans ce mode, les données copiées sont ajoutées aux données présentes
	 */
	ADDITIVE,
	/**
	 * Dans ce mode, les données copiées remplacent les données présentes
	 */
	EXCLUSIVE
}
