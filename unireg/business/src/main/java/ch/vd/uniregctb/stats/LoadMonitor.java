package ch.vd.uniregctb.stats;

/**
 * Interface implémentée par les entités capables de monitorer
 * la charge d'un système
 */
public interface LoadMonitor {

	/**
	 * @return la charge instantannée du système considéré
	 */
	int getChargeInstantannee();

	/**
	 * @return la moyenne de la charge sur les 5 dernières minutes
	 */
	double getMoyenneChargeCinqMinutes();
	
}
