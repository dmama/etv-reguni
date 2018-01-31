package ch.vd.unireg.interfaces.civil;

/**
 * Interface implémentée par un service civil qui en wrap un autre (proxy, cache...)
 */
public interface ServiceCivilServiceWrapper {

	/**
	 * @return le service immédiatement wrappé
	 */
	ServiceCivilRaw getTarget();

	/**
	 * @return le service wrappé en bout de chaîne
	 */
	ServiceCivilRaw getUltimateTarget();
}
