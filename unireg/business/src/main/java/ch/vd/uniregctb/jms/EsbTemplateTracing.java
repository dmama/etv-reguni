package ch.vd.uniregctb.jms;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Version spécialisée du template Esb qui relève les temps d'exécution des méthodes publiques.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EsbTemplateTracing extends EsbJmsTemplate implements DisposableBean {

	private StatsService statsService;

	private final Map<String, ServiceTracing> map = new HashMap<>();

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	private ServiceTracing get(EsbMessage message) {
		return get(message.getServiceDestination());
	}

	private ServiceTracing get(String destination) {
		ServiceTracing s = map.get(destination);
		if (s == null) {
			s = create(destination);
		}
		return s;
	}

	private ServiceTracing create(String destination) {
		synchronized (map) {
			ServiceTracing s = map.get(destination);
			if (s == null) {
				s = new ServiceTracing("EsbTemplate");
				map.put(destination, s);
				statsService.registerService(destination, s);
			}
			return s;
		}
	}

	@Override
	public void destroy() throws Exception {
		for (String s : map.keySet()) {
			statsService.unregisterService(s);
		}
	}

	@Override
	public void send(final EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		final long time = tracing.start();
		try {
			super.send(esbMessage);
		}
		finally {
			tracing.end(time, "send", new Object() {
				@Override
				public String toString() {
					return String.format("queue=%s, businessId='%s'", esbMessage.getServiceDestination(), esbMessage.getBusinessId());
				}
			});
		}
	}

	@Override
	public void sendInternal(final EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		final long time = tracing.start();
		try {
			super.sendInternal(esbMessage);
		}
		finally {
			tracing.end(time, "sendInternal", new Object() {
				@Override
				public String toString() {
					return String.format("queue=%s, businessId='%s'", esbMessage.getServiceDestination(), esbMessage.getBusinessId());
				}
			});
		}
	}

	@Override
	public EsbMessage receive(final String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receive(destinationName);
		}
		finally {
			tracing.end(time, "receive", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s", destinationName);
				}
			});
		}
	}

	@Override
	public EsbMessage receiveSelected(final String destinationName, final String messageSelector) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveSelected(destinationName, messageSelector);
		}
		finally {
			tracing.end(time, "receiveSelected", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s, selector='%s'", destinationName, messageSelector);
				}
			});
		}
	}

	@Override
	public EsbMessage receiveInternal(final String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveInternal(destinationName);
		}
		finally {
			tracing.end(time, "receiveInternal", new Object() {
				@Override
				public String toString() {
					return String.format("destinatioName=%s", destinationName);
				}
			});
		}
	}

	@Override
	public EsbMessage receiveSelectedInternal(final String destinationName, final String messageSelector) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveSelectedInternal(destinationName, messageSelector);
		}
		finally {
			tracing.end(time, "receiveSelectedInternal", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s, selector='%s'", destinationName, messageSelector);
				}
			});
		}
	}
}
