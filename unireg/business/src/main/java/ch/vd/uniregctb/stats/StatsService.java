package ch.vd.uniregctb.stats;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;
import net.sf.ehcache.Ehcache;

/**
 * Ce service centralise les informations statistiques des services de l'application.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface StatsService {

	void registerService(String serviceName, ServiceTracingInterface tracing);

	void registerCache(String serviceName, Ehcache cache);

	void unregisterService(String serviceName);

	void unregisterCache(String serviceName);

	ServiceStats getServiceStats(String serviceName);

	/**
	 * @return une tableau contenant les différents stats
	 */
	String buildStats();
}
