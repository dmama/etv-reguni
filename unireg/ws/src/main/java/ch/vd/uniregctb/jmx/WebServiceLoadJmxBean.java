package ch.vd.uniregctb.jmx;

/**
 * Interface du bean JMX de monitoring de la charge des web-services
 */
public interface WebServiceLoadJmxBean {

	/**
	 * @return la charge instantanée du service (= nombre d'appel actuellement en cours)
	 */
	int getLoad();

	/**
	 * @return la moyenne de la charge instantanée du service sur les 5 dernières minutes
	 */
	double getAverageLoad();
}
