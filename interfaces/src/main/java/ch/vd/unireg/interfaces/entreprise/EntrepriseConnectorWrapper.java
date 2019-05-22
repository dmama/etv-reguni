package ch.vd.unireg.interfaces.entreprise;

/**
 * Interface implémentée par un connecteur des entreprises qui en wrap un autre (proxy, cache...)
 */
public interface EntrepriseConnectorWrapper {

	/**
	 * @return le service immédiatement wrappé
	 */
	EntrepriseConnector getTarget();


	/**
	 * @return le service wrappé en bout de chaîne
	 */
	EntrepriseConnector getUltimateTarget();
}
