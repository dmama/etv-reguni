package ch.vd.uniregctb.webservices.tiers2.impl;

import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.management.counters.Counter;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.counters.ResponseTimeCounter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.stats.ServiceTracingInterface;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.ResponseTimeTracingCounter;

/**
 * Bean qui expose les informations de performances du web-service tiers2 (relevées par les intercepteur CXF) de manière compréhensible par
 * le service de tracing.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersWebServiceTracing implements ServiceTracingInterface, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "TiersWebService2";

	private CounterRepository counterRepository;
	private int lastRefreshCountersSize = 0;
	private ResponseTimeTracingCounter globalCounter;
	private final Map<String, ResponseTimeTracingCounter> counterByOperation = new HashMap<String, ResponseTimeTracingCounter>();
	private StatsService statsService;

	public void setCounterRepository(CounterRepository counterRepository) {
		this.counterRepository = counterRepository;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, this);
		}
	}

	@Override
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
				if (canonicalName.contains("service=\"{http://www.vd.ch/uniregctb/webservices/tiers2}TiersService\"")) {
					if (!canonicalName.contains("operation=")) {
						// temps de réponse général du service
						globalCounter = new ResponseTimeTracingCounter(counter);
					}
					else {
						// temps de réponse des méthodes particulières
						final String operationName = extractOperationName(canonicalName);
						counterByOperation.put(operationName, new ResponseTimeTracingCounter(counter));
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

	@Override
	public long getLastCallTime() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getLastCallTime();
	}

	@Override
	public long getRecentPing() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getRecentPing();
	}

	@Override
	public long getRecentTime() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getRecentTime();
	}

	@Override
	public long getRecentCount() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getRecentCount();
	}

	@Override
	public long getRecentItemsCount() {
		return getRecentCount();
	}

	@Override
	public long getRecentItemsPing() {
		return getRecentPing();
	}

	@Override
	public void onTick() {
		if (globalCounter != null) {
			globalCounter.onTick();
		}
		for (ResponseTimeTracingCounter compteurParOperation : counterByOperation.values()) {
			compteurParOperation.onTick();
		}
	}

	@Override
	public long getTotalPing() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getTotalPing();
	}

	@Override
	public long getTotalTime() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getTotalTime();
	}

	@Override
	public long getTotalCount() {
		refreshCounters();
		if (globalCounter == null) {
			return 0;
		}
		return globalCounter.getTotalCount();
	}

	@Override
	public long getTotalItemsCount() {
		return getTotalCount();
	}

	@Override
	public long getTotalItemsPing() {
		return getTotalPing();
	}

	@Override
	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		refreshCounters();
		return counterByOperation;
	}
}
