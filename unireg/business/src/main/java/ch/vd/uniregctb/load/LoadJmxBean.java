package ch.vd.uniregctb.load;

/**
 * Interface d'un bean JMX qui expose une charge instantanée et moyenne
 */
public interface LoadJmxBean {

	/**
	 * @return la charge instantanée (= nombre d'appels actuellement en cours)
	 */
	int getLoad();

	/**
	 * @return la moyenne de la charge instantanée sur les 5 dernières minutes
	 */
	double getAverageLoad();
}
