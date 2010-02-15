package ch.vd.uniregctb.stats;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;
import net.sf.ehcache.Ehcache;

/**
 * Ce service centralise les informations statistiques des services de l'application.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface StatsService {

	void registerRaw(String serviceName, ServiceTracingInterface tracing);

	void registerCached(String serviceName, Ehcache cache);

	void unregisterRaw(String serviceName);

	void unregisterCached(String serviceName);

	/**
	 * @return une tableau contenant les différents stats
	 */
	String buildStats();
}
