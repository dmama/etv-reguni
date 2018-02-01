package ch.vd.unireg.stats;

import java.util.Map;

import ch.vd.unireg.cache.UniregCacheInterface;

/**
 * Interface implémentée par le service de calcul des statistiques afin de pouvoir aller voir
 * dedans (pour exposer en JMX, par exemple, le détail)
 */
public interface StatsExposureInterface {

	/**
	 * @return une map indexée par nom des services enregistrés
	 */
	Map<String, ServiceTracingInterface> getServices();

	/**
	 * @return une map indexée par nom des caches enregistrés
	 */
	Map<String, UniregCacheInterface> getCaches();

	/**
	 * @return une map indexée par nom des moniteurs de charge enregistrés
	 */
	Map<String, LoadMonitor> getLoadMonitors();

	/**
	 * @return une map indexée par nom des moniteurs de job enregistrés
	 */
	Map<String, JobMonitor> getJobMonitors();
}
