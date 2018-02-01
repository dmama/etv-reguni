package ch.vd.unireg.tiers;

/**
 * Interface implémentée par une donnée pouvant provenir de différentes sources
 */
public interface Sourced<T extends Enum<T>> {

	/**
	 * @return La source de la donnée.
	 */
	T getSource();
}
