package ch.vd.unireg.interfaces.civil;

/**
 * Interface implémentée par un connecteur des individus qui en wrap un autre (proxy, cache...)
 */
public interface IndividuConnectorWrapper {

	/**
	 * @return le service immédiatement wrappé
	 */
	IndividuConnector getTarget();

	/**
	 * @return le service wrappé en bout de chaîne
	 */
	IndividuConnector getUltimateTarget();
}
