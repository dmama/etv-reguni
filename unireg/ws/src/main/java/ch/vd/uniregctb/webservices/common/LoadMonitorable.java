package ch.vd.uniregctb.webservices.common;

/**
 * Interface implémentée par les services dont on peut monitorer la charge
 */
public interface LoadMonitorable {

	/**
	 * @return la charge instantanée (par exemple le nombre d'appels actuellement en cours)
	 */
	int getChargeInstantanee();
}
