package ch.vd.unireg.jmx;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class JmxStatusChecker {

	private enum Environment {

		IN("slv2655v.etat-de-vaud.ch:50609", false),
		I2("ssv0309v.etat-de-vaud.ch:54609", false),
		TE("spip.etat-de-vaud.ch:50609", false),
		VA("slv2743v.etat-de-vaud.ch:30609", true),
		PP("slv2745v.etat-de-vaud.ch:34609", true),
		PR("ssv0296p.etat-de-vaud.ch:20609", true),
		PO("ssv0298v.etat-de-vaud.ch:38609", true);

		private final String hostWithPort;
		private final boolean withAuth;

		Environment(String hostWithPort, boolean withAuth) {
			this.hostWithPort = hostWithPort;
			this.withAuth = withAuth;
		}

		public String getHostWithPort() {
			return hostWithPort;
		}

		public boolean isWithAuth() {
			return withAuth;
		}
	}

	private static final String USER = "monitorRole";
	private static final String PWD = "...se renseigner...";

	private interface ValueFetcher<T> {
		Class<T> getResultingClass();
		T getValue(MBeanServerConnection mbeanServer) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException;
		String getValueDescription();
	}

	private interface AttributeSpecific {
		String getAttributeName();
	}

	private static abstract class AttributeSpecificFetcher<T> implements ValueFetcher<T>, AttributeSpecific {
		private final ObjectName objectName;

		protected AttributeSpecificFetcher(ObjectName objectName) {
			this.objectName = objectName;
		}

		@Override
		public final T getValue(MBeanServerConnection mbeanServer) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException {
			//noinspection unchecked
			return (T) mbeanServer.getAttribute(objectName, getAttributeName());
		}

		@Override
		public String getValueDescription() {
			return getAttributeName();
		}
	}

	private static abstract class IntegerFetcher extends AttributeSpecificFetcher<Integer> {
		protected IntegerFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public final Class<Integer> getResultingClass() {
			return Integer.class;
		}
	}

	private static class ThreadPoolCurrentSizeFetcher extends IntegerFetcher {
		public ThreadPoolCurrentSizeFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "currentThreadCount";
		}
	}

	private static class ThreadPoolBusySizeFetcher extends IntegerFetcher {
		public ThreadPoolBusySizeFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "currentThreadsBusy";
		}
	}

	private static abstract class StringFetcher extends AttributeSpecificFetcher<String> {
		protected StringFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public Class<String> getResultingClass() {
			return String.class;
		}
	}

	private static class ApplicationNameFetcher extends StringFetcher {
		public ApplicationNameFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "Description";
		}
	}

	private static class ApplicationStatusFetcher extends StringFetcher {
		public ApplicationStatusFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "StatusJSON";
		}
	}

	private static class ApplicationUptimeFetcher extends IntegerFetcher {
		public ApplicationUptimeFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "Uptime";
		}
	}

	private static class ApplicationVersionFetcher extends StringFetcher {
		public ApplicationVersionFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "Version";
		}
	}

	private static class NbReceivedMessagesFetcher extends IntegerFetcher {
		public NbReceivedMessagesFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "ReceivedMessages";
		}
	}

	private static abstract class BooleanFetcher extends AttributeSpecificFetcher<Boolean> {
		protected BooleanFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public Class<Boolean> getResultingClass() {
			return Boolean.class;
		}
	}

	private static class RunningFlagFetcher extends BooleanFetcher {
		public RunningFlagFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "Running";
		}
	}

	private static class DestinationNameFetcher extends StringFetcher {
		public DestinationNameFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "DestinationName";
		}
	}

	private static class MaxActiveFetcher extends IntegerFetcher {
		public MaxActiveFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "MaxActive";
		}
	}

	private static class NbActiveFetcher extends IntegerFetcher {
		public NbActiveFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbActive";
		}
	}

	private static class NbIdleFetcher extends IntegerFetcher {
		public NbIdleFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbIdle";
		}
	}

	private static abstract class DoubleFetcher extends AttributeSpecificFetcher<Double> {
		protected DoubleFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public Class<Double> getResultingClass() {
			return Double.class;
		}
	}

	private static class AverageLoadFetcher extends DoubleFetcher {
		public AverageLoadFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "AverageLoad";
		}
	}

	private static class LoadFetcher extends IntegerFetcher {
		public LoadFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "Load";
		}
	}

	private static class NbMeaningfullEventsReceivedFetcher extends IntegerFetcher {
		public NbMeaningfullEventsReceivedFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbMeaningfullEventsReceived";
		}
	}

	private static class NbIndividualsAwaitingTreatmentFetcher extends IntegerFetcher {
		public NbIndividualsAwaitingTreatmentFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbIndividualsAwaitingTreatment";
		}
	}

	private static class NbBatchEventsRejectedExceptionFetcher extends IntegerFetcher {
		public NbBatchEventsRejectedExceptionFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbBatchEventsRejectedException";
		}
	}

	private static class NbBatchEventsRejectedToErrorQueueFetcher extends IntegerFetcher {
		public NbBatchEventsRejectedToErrorQueueFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbBatchEventsRejectedToErrorQueue";
		}
	}

	private static class NbManualEventsRejectedExceptionFetcher extends IntegerFetcher {
		public NbManualEventsRejectedExceptionFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbManualEventsRejectedException";
		}
	}

	private static class NbManualEventsRejectedToErrorQueueFetcher extends IntegerFetcher {
		public NbManualEventsRejectedToErrorQueueFetcher(ObjectName objectName) {
			super(objectName);
		}

		@Override
		public String getAttributeName() {
			return "NbManualEventsRejectedToErrorQueue";
		}
	}

	public static void main(String args[]) throws Exception {

		if (args.length != 1) {
			System.err.println("One parameter must be given : " + Arrays.toString(Environment.values()));
			System.exit(1);
		}
		final Environment environment = Environment.valueOf(args[0]);

		final Map<ObjectName, Collection<ValueFetcher>> fetchers = new TreeMap<>();

		final String url = "service:jmx:rmi:///jndi/rmi://" + environment.getHostWithPort() + "/jmxrmi";
		final JMXServiceURL serviceUrl = new JMXServiceURL(url);
		final Map<String, String[]> env = new HashMap<>();
		if (environment.isWithAuth()) {
			final String[] credentials = { USER, PWD };
			env.put(JMXConnector.CREDENTIALS, credentials);
		}
		try (JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, env)) {
			final MBeanServerConnection mbeanConn = jmxConnector.getMBeanServerConnection();

			// catalina thread pools
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("Catalina:*,type=ThreadPool"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new ThreadPoolCurrentSizeFetcher(name),
					                                 new ThreadPoolBusySizeFetcher(name)));
				}
			}

			// application name, version and uptime
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.unireg*:name=Application,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new ApplicationNameFetcher(name),
					                                 new ApplicationVersionFetcher(name),
					                                 new ApplicationStatusFetcher(name),
					                                 new ApplicationUptimeFetcher(name)));
				}
			}

			// jms listeners
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.unireg*:*,type=JmsListeners"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new DestinationNameFetcher(name),
					                                 new RunningFlagFetcher(name),
					                                 new NbReceivedMessagesFetcher(name)));
				}
			}

			// oracle connections
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.unireg*:name=Oracle,type=Connections"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new MaxActiveFetcher(name),
					                                 new NbActiveFetcher(name),
					                                 new NbIdleFetcher(name)));
				}
			}

			// application load
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.unireg*:name=*Load,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new LoadFetcher(name),
					                                 new AverageLoadFetcher(name)));
				}
			}

			// web-services load
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.unireg*:name=*,resourceName=WebserviceLoad,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new LoadFetcher(name),
					                                 new AverageLoadFetcher(name)));
				}
			}

			// événement civils ech
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.unireg*:name=EvenementsCivils,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.asList(new NbMeaningfullEventsReceivedFetcher(name),
					                                 new NbIndividualsAwaitingTreatmentFetcher(name),
					                                 new NbBatchEventsRejectedExceptionFetcher(name),
					                                 new NbBatchEventsRejectedToErrorQueueFetcher(name),
					                                 new NbManualEventsRejectedExceptionFetcher(name),
					                                 new NbManualEventsRejectedToErrorQueueFetcher(name)));
				}

			}

			// all
			{
//				final Set<ObjectName> beanSet = mbeanConn.queryNames(null, null);
//				for (ObjectName name : beanSet) {
//					System.err.println(name.getCanonicalName());
//				}
			}

			// value fetching...
			for (Map.Entry<ObjectName, Collection<ValueFetcher>> fetch : fetchers.entrySet()) {
				final String name = fetch.getKey().getCanonicalName();
				System.out.println("Bean " + name);
				for (ValueFetcher fetcher : fetch.getValue()) {
					System.out.println('\t' + fetcher.getValueDescription() + ": " + fetcher.getValue(mbeanConn));
				}
			}
		}
	}
}
