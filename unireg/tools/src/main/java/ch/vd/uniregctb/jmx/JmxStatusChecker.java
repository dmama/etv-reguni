package ch.vd.uniregctb.jmx;

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

	private static enum Environment {

		IN("ssv0309v.etat-de-vaud.ch:50609", false),
		I2("ssv0309v.etat-de-vaud.ch:54609", false),
		TE("spip.etat-de-vaud.ch:50609", false),
		VA("ssv0298v.etat-de-vaud.ch:30609", true),
		PP("ssv0298v.etat-de-vaud.ch:34609", true),
		PR("ssv0296p.etat-de-vaud.ch:20609", true),
		PO("ssv0298v.etat-de-vaud.ch:38609", true);

		private final String url;
		private final boolean withAuth;

		private Environment(String url, boolean withAuth) {
			this.url = url;
			this.withAuth = withAuth;
		}

		public String getUrl() {
			return url;
		}

		public boolean isWithAuth() {
			return withAuth;
		}
	}

	private static final String USER = "monitorRole";
	private static final String PWD = "...se renseigner...";

	private static interface ValueFetcher<T> {
		Class<T> getResultingClass();
		T getValue(MBeanServerConnection mbeanServer, ObjectName name) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException;
		String getValueDescription();
	}

	private static interface AttributeSpecific {
		String getAttributeName();
	}

	private static abstract class AttributeSpecificFetcher<T> implements ValueFetcher<T>, AttributeSpecific {
		@Override
		public final T getValue(MBeanServerConnection mbeanServer, ObjectName name) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException {
			//noinspection unchecked
			return (T) mbeanServer.getAttribute(name, getAttributeName());
		}

		@Override
		public String getValueDescription() {
			return getAttributeName();
		}
	}

	private static abstract class IntegerFetcher extends AttributeSpecificFetcher<Integer> {
		@Override
		public final Class<Integer> getResultingClass() {
			return Integer.class;
		}
	}

	private static class ThreadPoolCurrentSizeFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "currentThreadCount";
		}
	}

	private static class ThreadPoolBusySizeFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "currentThreadsBusy";
		}
	}

	private static abstract class StringFetcher extends AttributeSpecificFetcher<String> {
		@Override
		public Class<String> getResultingClass() {
			return String.class;
		}
	}

	private static class ApplicationNameFetcher extends StringFetcher {
		@Override
		public String getAttributeName() {
			return "Description";
		}
	}

	private static class ApplicationStatusFetcher extends StringFetcher {
		@Override
		public String getAttributeName() {
			return "StatusJSON";
		}
	}

	private static class ApplicationUptimeFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "Uptime";
		}
	}

	private static class ApplicationVersionFetcher extends StringFetcher {
		@Override
		public String getAttributeName() {
			return "Version";
		}
	}

	private static class NbReceivedMessagesFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "ReceivedMessages";
		}
	}

	private static abstract class BooleanFetcher extends AttributeSpecificFetcher<Boolean> {
		@Override
		public Class<Boolean> getResultingClass() {
			return Boolean.class;
		}
	}

	private static class RunningFlagFetcher extends BooleanFetcher {
		@Override
		public String getAttributeName() {
			return "Running";
		}
	}

	private static class DestinationNameFetcher extends StringFetcher {
		@Override
		public String getAttributeName() {
			return "DestinationName";
		}
	}

	private static class MaxActiveFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "MaxActive";
		}
	}

	private static class NbActiveFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbActive";
		}
	}

	private static class NbIdleFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbIdle";
		}
	}

	private static abstract class DoubleFetcher extends AttributeSpecificFetcher<Double> {
		@Override
		public Class<Double> getResultingClass() {
			return Double.class;
		}
	}

	private static class AverageLoadFetcher extends DoubleFetcher {
		@Override
		public String getAttributeName() {
			return "AverageLoad";
		}
	}

	private static class LoadFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "Load";
		}
	}

	private static class NbMeaningfullEventsReceivedFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbMeaningfullEventsReceived";
		}
	}

	private static class NbIndividualsAwaitingTreatmentFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbIndividualsAwaitingTreatment";
		}
	}

	private static class NbBatchEventsRejectedExceptionFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbBatchEventsRejectedException";
		}
	}

	private static class NbBatchEventsRejectedToErrorQueueFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbBatchEventsRejectedToErrorQueue";
		}
	}

	private static class NbManualEventsRejectedExceptionFetcher extends IntegerFetcher {
		@Override
		public String getAttributeName() {
			return "NbManualEventsRejectedException";
		}
	}

	private static class NbManualEventsRejectedToErrorQueueFetcher extends IntegerFetcher {
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

		final String url = "service:jmx:rmi:///jndi/rmi://" + environment.getUrl() + "/jmxrmi";
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
					fetchers.put(name, Arrays.<ValueFetcher>asList(new ThreadPoolCurrentSizeFetcher(), new ThreadPoolBusySizeFetcher()));
				}
			}

			// application name, version and uptime
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.uniregctb*:name=Application,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.<ValueFetcher>asList(new ApplicationNameFetcher(),
					                                               new ApplicationVersionFetcher(),
					                                               new ApplicationStatusFetcher(),
					                                               new ApplicationUptimeFetcher()));
				}
			}

			// jms listeners
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.uniregctb*:*,type=JmsListeners"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.<ValueFetcher>asList(new DestinationNameFetcher(),
					                                               new RunningFlagFetcher(),
					                                               new NbReceivedMessagesFetcher()));
				}
			}

			// oracle connections
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.uniregctb*:name=Oracle,type=Connections"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.<ValueFetcher>asList(new MaxActiveFetcher(),
					                                               new NbActiveFetcher(),
					                                               new NbIdleFetcher()));
				}
			}

			// application load
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.uniregctb*:name=*Load,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.<ValueFetcher>asList(new LoadFetcher(),
					                                               new AverageLoadFetcher()));
				}
			}

			// web-services load
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.uniregctb*:name=*,resourceName=WebserviceLoad,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.<ValueFetcher>asList(new LoadFetcher(),
					                                               new AverageLoadFetcher()));
				}
			}

			// événement civils ech
			{
				final Set<ObjectName> beanSet = mbeanConn.queryNames(new ObjectName("ch.vd.uniregctb*:name=EvenementsCivils,type=Monitoring"), null);
				for (ObjectName name : beanSet) {
					fetchers.put(name, Arrays.<ValueFetcher>asList(new NbMeaningfullEventsReceivedFetcher(),
					                                               new NbIndividualsAwaitingTreatmentFetcher(),
					                                               new NbBatchEventsRejectedExceptionFetcher(),
					                                               new NbBatchEventsRejectedToErrorQueueFetcher(),
					                                               new NbManualEventsRejectedExceptionFetcher(),
					                                               new NbManualEventsRejectedToErrorQueueFetcher()));
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
					System.out.println('\t' + fetcher.getValueDescription() + ": " + fetcher.getValue(mbeanConn, fetch.getKey()));
				}
			}
		}
	}
}
