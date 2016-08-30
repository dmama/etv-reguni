package ch.vd.uniregctb.stats;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ServiceTracingFactory implements FactoryBean<ServiceTracing>, InitializingBean, DisposableBean {

	private String serviceName;
	private boolean detailedLogging = true;
	private StatsService statsService;

	private ServiceTracing tracing;

	@SuppressWarnings("UnusedDeclaration")
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDetailedLogging(boolean detailedLogging) {
		this.detailedLogging = detailedLogging;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.isBlank(serviceName)) {
			throw new IllegalArgumentException("serviceName must be set!");
		}
		tracing = new ServiceTracing(serviceName, detailedLogging);
		if (statsService != null) {
			statsService.registerService(serviceName, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(serviceName);
		}
	}

	@Override
	public ServiceTracing getObject() throws Exception {
		return tracing;
	}

	@Override
	public Class<?> getObjectType() {
		return ServiceTracing.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
