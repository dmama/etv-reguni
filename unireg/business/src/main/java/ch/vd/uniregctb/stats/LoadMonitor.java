package ch.vd.uniregctb.stats;

/**
 * Interface implémentée par les entités capables de monitorer
 * la charge d'un système
 */
public interface LoadMonitor {

	/**
	 * @return la charge instantanée du système considéré
	 */
	int getChargeInstantanee();

	/**
	 * @return la moyenne de la charge sur les 5 dernières minutes
	 */
	double getMoyenneChargeCinqMinutes();
	
}
