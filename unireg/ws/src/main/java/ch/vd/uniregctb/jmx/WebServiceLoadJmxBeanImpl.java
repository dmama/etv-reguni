package ch.vd.uniregctb.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.stats.LoadMonitor;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.LoadAverager;
import ch.vd.uniregctb.webservices.common.LoadMonitorable;

/**
 * Bean JMX de monitoring de la charge des web-services
 */
@ManagedResource
public class WebServiceLoadJmxBeanImpl implements WebServiceLoadJmxBean, InitializingBean, DisposableBean {

	private Map<String, LoadMonitorable> services;

	private Map<String, LoadAverager> averagers;

	private StatsService statsService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServices(Map<String, LoadMonitorable> services) {
		this.services = services;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(services);
		if (services.size() > 0) {
			averagers = new HashMap<String, LoadAverager>(services.size());
			for (Map.Entry<String, LoadMonitorable> entry : services.entrySet()) {
				final String serviceName = entry.getKey();
				final LoadMonitorable service = entry.getValue();

				final LoadAverager averager = new LoadAverager(service, serviceName, 600, 500); // 5 minutes, 2 fois par seconde
				averagers.put(serviceName, averager);
				averager.start();

				statsService.registerLoadMonitor(serviceName, new LoadMonitor() {

					@Override
					public int getLoad() {
						return service.getLoad();
					}

					@Override
					public double getFiveMinuteAverageLoad() {
						return averager.getAverageLoad();
					}
				});
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		if (averagers != null && averagers.size() > 0) {
			for (LoadAverager averager : averagers.values()) {
				if (averager != null) {
					averager.stop();
				}
			}
			averagers = null;
		}

		for (String serviceName : services.keySet()) {
			statsService.unregisterLoadMonitor(serviceName);
		}
		services = null;
	}

	@Override
	@ManagedAttribute
	public Map<String, Integer> getLoad() {
		final Map<String, Integer> map = new TreeMap<String, Integer>();
		for (Map.Entry<String, LoadMonitorable> entry : services.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getLoad());
		}
		return map;
	}

	@Override
	@ManagedAttribute
	public Map<String, Double> getAverageLoad() {
		final Map<String, Double> map = new TreeMap<String, Double>();
		for (Map.Entry<String, LoadAverager> entry : averagers.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getAverageLoad());
		}
		return map;
	}

	@Override
	@ManagedOperation
	public Integer getLoad(String serviceName) {
		final LoadMonitorable service = services.get(serviceName);
		return service != null ? service.getLoad() : null;
	}

	@Override
	@ManagedOperation
	public Double getAverageLoad(String serviceName) {
		final LoadAverager averager = averagers.get(serviceName);
		return averager != null ? averager.getAverageLoad() : null;
	}
}
