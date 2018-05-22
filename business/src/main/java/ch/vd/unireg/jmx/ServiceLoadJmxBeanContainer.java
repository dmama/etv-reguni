package ch.vd.unireg.jmx;

import javax.management.ObjectName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.MBeanExportOperations;

import ch.vd.unireg.load.DetailedLoadJmxBeanImpl;
import ch.vd.unireg.load.LoadJmxBean;
import ch.vd.unireg.load.LoadJmxBeanImpl;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadMonitorable;
import ch.vd.unireg.stats.StatsService;

/**
 * Bean JMX de monitoring de la charge de services (web-services, accès jms...)
 */
public class ServiceLoadJmxBeanContainer implements InitializingBean, DisposableBean {

	private Map<String, LoadMonitorable> services;
	
	private Map<String, LoadJmxBean> jmxBeans;

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
		if (services == null) {
			throw new IllegalArgumentException();
		}
		if (!services.isEmpty()) {
			jmxBeans = new HashMap<>(services.size());
			for (Map.Entry<String, LoadMonitorable> entry : services.entrySet()) {
				final String serviceName = entry.getKey();
				final LoadMonitorable service = entry.getValue();

				final String name = String.format("%s%s", objectNamePrefix, serviceName);
				final LoadJmxBean bean;
				if (service instanceof DetailedLoadMonitorable) {
					bean = new DetailedLoadJmxBeanImpl(serviceName, (DetailedLoadMonitorable) service, statsService);
				}
				else {
					bean = new LoadJmxBeanImpl<>(serviceName, service, statsService);
				}

				jmxBeans.put(serviceName, bean);
				exporter.registerManagedResource(bean, ObjectName.getInstance(name));
			}
		}
		else {
			jmxBeans = Collections.emptyMap();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (!jmxBeans.isEmpty()) {
			for (Map.Entry<String, LoadJmxBean> entry : jmxBeans.entrySet()) {
				// ces beans ont été créés à la main, il faut donc les détruire à la main
				if (entry.getValue() instanceof DisposableBean) {
					final DisposableBean bean = (DisposableBean) entry.getValue();
					bean.destroy();
				}
			}
		}
	}
}
