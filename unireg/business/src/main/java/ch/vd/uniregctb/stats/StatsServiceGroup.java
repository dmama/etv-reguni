package ch.vd.uniregctb.stats;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Version spéciale du service des statistiques qui permet de regrouper plusieurs sous-interfaces dans un groupe.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class StatsServiceGroup implements StatsService, ServiceTracingInterface, InitializingBean, DisposableBean {

	private StatsService statsService;
	private String groupName;
	private final Map<String, ServiceTracingInterface> subServices = new HashMap<String, ServiceTracingInterface>();

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public void registerService(String serviceName, ServiceTracingInterface tracing) {
		synchronized (subServices) {
			subServices.put(serviceName, tracing);
		}
	}

	@Override
	public void registerCache(String serviceName, UniregCacheInterface cache) {
		throw new NotImplementedException();
	}

	@Override
	public void registerLoadMonitor(String serviceName, LoadMonitor monitor) {
		throw new NotImplementedException();
	}

	@Override
	public void unregisterService(String serviceName) {
		synchronized (subServices) {
			subServices.remove(serviceName);
		}
	}

	@Override
	public void unregisterCache(String serviceName) {
		throw new NotImplementedException();
	}

	@Override
	public void unregisterLoadMonitor(String serviceName) {
		throw new NotImplementedException();
	}

	@Override
	public long getLastCallTime() {
		long last = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				last = Math.max(last, s.getLastCallTime());
			}
		}
		return last;
	}

	@Override
	public long getTotalTime() {
		long total = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				total += s.getTotalTime();
			}
		}
		return total;
	}

	@Override
	public long getTotalPing() {
		long total = 0;
		long count = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				final long p = s.getTotalPing();
				if (p > 0) {
					total += p;
					count++;
				}
			}
		}
		return count > 0 ? (total / count) : 0;
	}

	@Override
	public long getTotalCount() {
		long count = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				count += s.getTotalCount();
			}
		}
		return count;
	}

	@Override
	public long getRecentTime() {
		long recent = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				recent += s.getRecentTime();
			}
		}
		return recent;
	}

	@Override
	public long getRecentPing() {
		long total = 0;
		long count = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				final long p = s.getRecentPing();
				if (p > 0) {
					total += p;
					count++;
				}
			}
		}
		return count > 0 ? (total / count) : 0;
	}

	@Override
	public long getRecentCount() {
		long count = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				count += s.getRecentCount();
			}
		}
		return count;
	}

	@Override
	public void onTick() {
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				s.onTick();
			}
		}
	}

	@Override
	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return subServices;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(groupName, this);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(groupName);
		}
	}

	@Override
	public String buildStats() {
		throw new NotImplementedException();
	}

	@Override
	public ServiceStats getServiceStats(String serviceName) {
		throw new NotImplementedException();
	}
}
