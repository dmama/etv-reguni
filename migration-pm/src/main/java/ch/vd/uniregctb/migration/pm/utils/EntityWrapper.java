package ch.vd.uniregctb.migration.pm.utils;

/**
 * Interface implémentée par les wrappers (façades et autres choses)
 * @param <T> type de l'entité wrappée
 */
public interface EntityWrapper<T> {

	/**
	 * @return l'entité wrappée
	 */
	T getWrappedEntity();
}
