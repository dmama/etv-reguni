package ch.vd.uniregctb.stats;

import ch.vd.uniregctb.cache.UniregCacheInterface;

public class MockStatsService implements StatsService {

	@Override
	public void registerService(String serviceName, ServiceTracingInterface tracing) {
	}

	@Override
	public void registerCache(String serviceName, UniregCacheInterface cache) {
	}

	@Override
	public void registerLoadMonitor(String serviceName, LoadMonitor monitor) {
	}

	@Override
	public void unregisterService(String serviceName) {
	}

	@Override
	public void unregisterCache(String serviceName) {
	}

	@Override
	public void unregisterLoadMonitor(String serviceName) {
	}

	@Override
	public ServiceStats getServiceStats(String serviceName) {
		return null;
	}

	@Override
	public String buildStats() {
		return "Not implemented in Mock...";
	}
}
