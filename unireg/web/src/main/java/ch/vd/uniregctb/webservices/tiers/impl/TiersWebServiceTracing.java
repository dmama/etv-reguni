package ch.vd.uniregctb.webservices.tiers.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import ch.vd.uniregctb.stats.StatsService;
import org.apache.cxf.management.counters.Counter;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.counters.ResponseTimeCounter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Bean qui expose les informations de performances du web-service tiers2 (relevées par les intercepteur CXF) de manière compréhensible par
 * le service de tracing.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersWebServiceTracing implements ServiceTracingInterface, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "TiersWebService";

	private CounterRepository counterRepository;
	private int lastRefreshCountersSize = 0;
	private TracingCounter globalCounter;
	private final Map<String, TracingCounter> counterByOperation = new HashMap<String, TracingCounter>();
	private StatsService statsService;

	public void setCounterRepository(CounterRepository counterRepository) {
		this.counterRepository = counterRepository;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, this);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	private void refreshCounters() {
		final Map<ObjectName, Counter> counters = counterRepository.getCounters();
		if (globalCounter == null || lastRefreshCountersSize != counters.size()) { // lazy init/refresh
			for (Map.Entry<ObjectName, Counter> e : counters.entrySet()) {
				final String canonicalName = e.getKey().getCanonicalName();
				final ResponseTimeCounter counter = (ResponseTimeCounter) e.getValue();
				if (canonicalName.contains("service=\"{http://www.vd.ch/uniregctb/webservices/tiers}TiersService\"")) {
					if (!canonicalName.contains("operation=")) {
						// temps de réponse général du service
						globalCounter = new TracingCounter(counter);
					}
					else {
						// temps de réponse des méthodes particulières
						final String operationName = extractOperationName(canonicalName);
						counterByOperation.put(operationName, new TracingCounter(counter));
					}
				}
			}
			lastRefreshCountersSize = counters.size();
		}
	}

	private static final Pattern operationPattern = Pattern.compile(".*operation=\"\\{(?:.*?)\\}(.*?)\",.*");

	private String extractOperationName(String canonicalName) {
		Matcher m = operationPattern.matcher(canonicalName);
		if (m.matches()) {
			return m.group(1);
		}
		return "<unknown>";
	}

	public long getLastCallTime() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getLastCallTime();
	}

	public long getRecentPing() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getRecentPing();
	}

	public long getRecentTime() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getRecentTime();
	}

	public long getRecentCount() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getRecentCount();
	}

	public long getTotalPing() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getTotalPing();
	}

	public long getTotalTime() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getTotalTime();
	}

	public long getTotalCount() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getTotalCount();
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		refreshCounters();
		return counterByOperation;
	}

	/**
	 * Permet d'adapter un ResponseTimeCounter à l'interface ServiceTracingInterface.
	 */
	private static class TracingCounter implements ServiceTracingInterface {

		private final ResponseTimeCounter counter;

		public TracingCounter(ResponseTimeCounter counter) {
			this.counter = counter;
		}

		public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
			return null;
		}

		public long getLastCallTime() {
			return 0;
		}

		public long getRecentPing() {
			return getTotalPing();
		}

		public long getRecentTime() {
			return getTotalTime();
		}

		public long getRecentCount() {
			return getTotalCount();
		}

		public long getTotalPing() {
			if (counter == null) {
				return 0;
			}

			long value = counter.getAvgResponseTime().longValue();
			return value / 1000;
		}

		public long getTotalTime() {
			if (counter == null) {
				return 0;
			}

			final long numInvocations = counter.getNumInvocations().longValue();
			long totalPing = getTotalPing();
			return numInvocations * totalPing;
		}

		public long getTotalCount() {
			if (counter == null) {
				return 0;
			}

			return counter.getNumInvocations().longValue();
		}
	}
}
