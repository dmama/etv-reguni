package ch.vd.uniregctb.stats;

import ch.vd.uniregctb.cache.UniregCacheInterface;

/**
 * Ce service centralise les informations statistiques des services de l'application.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface StatsService {

	void registerService(String serviceName, ServiceTracingInterface tracing);

	void registerCache(String serviceName, UniregCacheInterface cache);

	void registerLoadMonitor(String serviceName, LoadMonitor monitor);

	void unregisterService(String serviceName);

	void unregisterCache(String serviceName);

	void unregisterLoadMonitor(String serviceName);

	ServiceStats getServiceStats(String serviceName);

	/**
	 * @return une tableau contenant les différents stats
	 */
	String buildStats();
}
