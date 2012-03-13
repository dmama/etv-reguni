package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.stats.LoadMonitor;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.LoadAverager;
import ch.vd.uniregctb.webservices.common.LoadMonitorable;

@ManagedResource
public class WebServiceLoadJmxBeanImpl<T extends LoadMonitorable> implements WebServiceLoadJmxBean {

	protected final T service;

	private final LoadAverager averager;

	public WebServiceLoadJmxBeanImpl(String serviceName, final T service, StatsService statsService) {
		this.service = service;
		this.averager = new LoadAverager(service, serviceName, 600, 500);   // 5 minutes, 2 fois par seconde
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

	@ManagedAttribute
	@Override
	public int getLoad() {
		return service.getLoad();
	}

	@ManagedAttribute
	@Override
	public double getAverageLoad() {
		return averager.getAverageLoad();
	}
}
