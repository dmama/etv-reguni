package ch.vd.uniregctb.stats;

import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Ehcache;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.NotImplementedException;
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

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void registerService(String serviceName, ServiceTracingInterface tracing) {
		synchronized (subServices) {
			subServices.put(serviceName, tracing);
		}
	}

	public void registerCache(String serviceName, Ehcache cache) {
		throw new NotImplementedException();
	}

	public void registerLoadMonitor(String serviceName, LoadMonitor monitor) {
		throw new NotImplementedException();
	}

	public void unregisterService(String serviceName) {
		synchronized (subServices) {
			subServices.remove(serviceName);
		}
	}

	public void unregisterCache(String serviceName) {
		throw new NotImplementedException();
	}

	public void unregisterLoadMonitor(String serviceName) {
		throw new NotImplementedException();
	}

	public long getLastCallTime() {
		long last = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				last = Math.max(last, s.getLastCallTime());
			}
		}
		return last;
	}

	public long getTotalTime() {
		long total = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				total += s.getTotalTime();
			}
		}
		return total;
	}

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

	public long getTotalCount() {
		long count = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				count += s.getTotalCount();
			}
		}
		return count;
	}

	public long getRecentTime() {
		long recent = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				recent += s.getRecentTime();
			}
		}
		return recent;
	}

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

	public long getRecentCount() {
		long count = 0;
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				count += s.getRecentCount();
			}
		}
		return count;
	}

	public void onTick() {
		synchronized (subServices) {
			for (ServiceTracingInterface s : subServices.values()) {
				s.onTick();
			}
		}
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return subServices;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(groupName, this);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(groupName);
		}
	}

	public String buildStats() {
		throw new NotImplementedException();
	}

	public ServiceStats getServiceStats(String serviceName) {
		throw new NotImplementedException();
	}
}
