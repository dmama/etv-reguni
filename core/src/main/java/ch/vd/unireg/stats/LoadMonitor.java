package ch.vd.unireg.stats;

/**
 * Interface implémentée par les entités capables de monitorer
 * la charge d'un système
 */
public interface LoadMonitor {

	/**
	 * @return la charge instantanée du système considéré
	 */
	int getLoad();

	/**
	 * @return la moyenne de la charge sur les 5 dernières minutes
	 */
	double getFiveMinuteAverageLoad();

	/**
	 * @return l'entité monitorée
	 */
	LoadMonitorable getMonitorable();

}
