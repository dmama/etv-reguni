package ch.vd.uniregctb.jms;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class EsbMessageTracingFactoryImpl implements EsbMessageTracingFactory, InitializingBean, DisposableBean {

	private static final String TRACING_SERVICE_NAME = "EsbMessage";

	private final ServiceTracing tracing = new ServiceTracing(TRACING_SERVICE_NAME);

	private StatsService statsService;

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(TRACING_SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(TRACING_SERVICE_NAME);
		}
	}

	public EsbMessage wrap(EsbMessage src) {
		return new EsbMessageTracingFacade(src, tracing);
	}
}