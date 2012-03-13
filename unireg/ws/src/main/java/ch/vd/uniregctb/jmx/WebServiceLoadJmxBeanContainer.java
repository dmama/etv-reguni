package ch.vd.uniregctb.jmx;

import javax.management.ObjectName;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.MBeanExportOperations;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.DetailedLoadMonitorable;
import ch.vd.uniregctb.webservices.common.LoadMonitorable;

/**
 * Bean JMX de monitoring de la charge des web-services
 */
public class WebServiceLoadJmxBeanContainer implements InitializingBean {

	private Map<String, LoadMonitorable> services;

	private StatsService statsService;

	private MBeanExportOperations exporter;
	
	private String objectNamePrefix;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServices(Map<String, LoadMonitorable> services) {
		this.services = services;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExporter(MBeanExportOperations exporter) {
		this.exporter = exporter;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setObjectNamePrefix(String objectNamePrefix) {
		this.objectNamePrefix = objectNamePrefix;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(services);
		if (!services.isEmpty()) {
			for (Map.Entry<String, LoadMonitorable> entry : services.entrySet()) {
				final String serviceName = entry.getKey();
				final LoadMonitorable service = entry.getValue();
				
				final String name = String.format("%s%s", objectNamePrefix, serviceName);
				final WebServiceLoadJmxBean bean;
				if (service instanceof DetailedLoadMonitorable) {
					bean = new WebServiceDetailedLoadJmxBeanImpl(serviceName, (DetailedLoadMonitorable) service, statsService);
				}
				else {
					bean = new WebServiceLoadJmxBeanImpl(serviceName, service, statsService);
				}
				exporter.registerManagedResource(bean, ObjectName.getInstance(name));
			}
		}
	}
}
