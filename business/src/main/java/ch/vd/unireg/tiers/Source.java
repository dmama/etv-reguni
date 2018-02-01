package ch.vd.unireg.tiers;

/**
 * Enumération de sources pour caractériser des données.
 *
 * Cas simple, fiscal (unireg) ou civil (RCEnt, RCPers, ...).
 *
 * @author Raphaël Marmier, 2016-01-06, <raphael.marmier@vd.ch>
 */
public enum Source {

	/**
	 * Registre civil = registre cantonal des entreprises
	 */
	CIVILE,

	/**
	 * Registre fiscal = unireg
	 */
	FISCALE
}
