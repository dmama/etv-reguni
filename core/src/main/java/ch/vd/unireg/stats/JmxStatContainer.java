package ch.vd.unireg.stats;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.jmx.export.MBeanExportOperations;

public class JmxStatContainer implements SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(JmxStatContainer.class);

	private StatsExposureInterface statsService;
	private MBeanExportOperations exporter;
	private volatile boolean running;

	private String objectName;

	public void setStatsService(StatsExposureInterface statsService) {
		this.statsService = statsService;
	}

	public void setExporter(MBeanExportOperations exporter) {
		this.exporter = exporter;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		registerManagedResource(objectName, new ServiceStatsJmxBean(statsService));
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
		unregisterManagedResource(objectName);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		// après tous les autres, i.e. quand tous les services se sont enregistrés
		return Integer.MAX_VALUE;
	}

	private void registerManagedResource(String name, Object resource) {
		try {
			exporter.registerManagedResource(resource, ObjectName.getInstance(name));
		}
		catch (MalformedObjectNameException e) {
			LOGGER.error("Mauvais algorithme de formation du nom du bean JMX", e);
		}
	}

	private void unregisterManagedResource(String name) {
		try {
			exporter.unregisterManagedResource(ObjectName.getInstance(name));
		}
		catch (MalformedObjectNameException e) {
			LOGGER.error("Mauvais algorithme de formation du nom du bean JMX", e);
		}
	}

	/**
	 * Classe de base des Beans JMX exposés
	 * @param <T> le type de service surveillé
	 */
	private abstract static class StatsJmxBean<T> implements DynamicMBean {

		protected final Map<String, T> targets;
		protected final TabularType dataType;

		protected StatsJmxBean(Map<String, T> targets, TabularType dataType) {
			this.targets = new TreeMap<>(targets);
			this.dataType = dataType;
		}

		/**
		 * A implémenter par les sous-classes pour récupérer les informations à exposer
		 * @param target le service surveillé
		 * @return les données exposées du services
		 * @throws OpenDataException en cas de souci d'instanciation des données
		 */
		protected abstract TabularData getTargetData(T target) throws OpenDataException;

		@Override
		public final TabularData getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
			final T target = targets.get(attribute);
			if (target == null) {
				throw new AttributeNotFoundException("No registered target with name " + attribute);
			}
			try {
				return getTargetData(target);
			}
			catch (OpenDataException e) {
				LOGGER.error("Problème à la génération des données à exposer", e);
				throw new MBeanException(e);
			}
		}

		@Override
		public final void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
			// ne devrait jamais être appelé... aucun attribute n'est indiqué comme isWritable...
			throw new NotImplementedException("");
		}

		@Override
		public final AttributeList getAttributes(String[] attributes) {
			final AttributeList list = new AttributeList(attributes.length);
			for (String attr : attributes) {
				final T target = targets.get(attr);
				if (target != null) {
					try {
						list.add(new Attribute(attr, getTargetData(target)));
					}
					catch (OpenDataException e) {
						LOGGER.error("Problème à la génération des données à exposer", e);
						throw new RuntimeException(e);
					}
				}
			}
			return list;
		}

		@Override
		public final AttributeList setAttributes(AttributeList attributes) {
			// ne devrait jamais être appelé... aucun attribute n'est indiqué comme isWritable...
			throw new NotImplementedException("");
		}

		@Override
		public final Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
			// ne devrait jamais être appelé... aucune opération exposée
			throw new NotImplementedException("");
		}

		@Override
		public final MBeanInfo getMBeanInfo() {
			final List<OpenMBeanAttributeInfo> attrList = new LinkedList<>();
			for (Map.Entry<String, T> service : targets.entrySet()) {
				attrList.add(new OpenMBeanAttributeInfoSupport(service.getKey(), "Statistiques " + service.getKey(), dataType, true, false, false));
			}

			final OpenMBeanAttributeInfo[] attrs = attrList.toArray(new OpenMBeanAttributeInfo[0]);
			return new OpenMBeanInfoSupport(getClass().getName(), "Statistiques d'utilisation", attrs, null, null, null);
		}
	}

	private static final class ServiceStatsJmxBean extends StatsJmxBean<ServiceTracingInterface> {

		private static final String[] ROW_NAMES = {
				"scope",
				"totalCount", "totalAvgTime", "totalItems", "totalAvgTimePerItem",
				"recentCount", "recentAvgTime", "recentItems", "recentAvgTimePerItem"
		};

		private static final String[] ROW_INDEX = {ROW_NAMES[0]};
		private static final String[] DESCRIPTIONS = {
				"Service name",
				"Number of service calls since application start", "Average call duration since application start", "Returned items since application start", "Average item number per call since application start",
				"Number of recent service calls", "Average recent call duration", "Recently returned items", "Average recent item number per call"
		};

		public ServiceStatsJmxBean(StatsExposureInterface statsService) {
			super(statsService.getServices(), buildDataType());
		}

		private static TabularType buildDataType() {
			try {
				final OpenType<?>[] types = {SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG};
				final CompositeType rowType = new CompositeType("ServicePerformance", "Service performance description", ROW_NAMES, DESCRIPTIONS, types);
				return new TabularType("ServicePerformance", "All services performances", rowType, ROW_INDEX);
			}
			catch (OpenDataException e) {
				throw new RuntimeException(e);
			}
		}

		protected TabularData getTargetData(ServiceTracingInterface service) throws OpenDataException {
			final TabularData data = new TabularDataSupport(dataType);

			final CompositeType rowType = dataType.getRowType();

			// global times
			data.put(new CompositeDataSupport(rowType, ROW_NAMES, buildValues("Global", service)));

			// specific methods/sub-categories
			for (Map.Entry<String, ? extends ServiceTracingInterface> detail : service.getDetailedData().entrySet()) {
				data.put(new CompositeDataSupport(rowType, ROW_NAMES, buildValues(detail.getKey(), detail.getValue())));
			}

			return data;
		}

		private static Object[] buildValues(String name, ServiceTracingInterface service) {
			return new Object[] {
					name,
					service.getTotalCount(), service.getTotalPing(), service.getTotalItemsCount(), service.getTotalItemsPing(),
					service.getRecentCount(), service.getRecentPing(), service.getRecentItemsCount(), service.getRecentItemsPing()
			};
		}
	}
}
