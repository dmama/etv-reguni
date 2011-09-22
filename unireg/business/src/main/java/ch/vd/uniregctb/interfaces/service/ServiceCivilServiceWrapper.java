package ch.vd.uniregctb.interfaces.service;

/**
 * Interface implémentée par un service civil qui en wrap un autre (proxy, cache...)
 */
public interface ServiceCivilServiceWrapper {

	/**
	 * @return le service immédiatement wrappé
	 */
	ServiceCivilService getTarget();

	/**
	 * @return le service wrappé en bout de chaîne
	 */
	ServiceCivilService getUltimateTarget();
}
