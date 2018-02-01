package ch.vd.unireg.jms;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

/**
 * Version spécialisée du template Esb qui relève les temps d'exécution des méthodes publiques.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EsbTemplateTracing extends EsbJmsTemplate implements DisposableBean {

	private StatsService statsService;
	private String destinationSuffix = null;

	private final Map<String, ServiceTracing> map = new HashMap<>();

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setDestinationSuffix(String destinationSuffix) {
		this.destinationSuffix = destinationSuffix;
	}

	private ServiceTracing get(EsbMessage message) {
		return getOrCreate(getLoggedDestination(message.getServiceDestination()));
	}

	private String getLoggedDestination(String destination) {
		return destinationSuffix == null ? destination : String.format("%s%s", destination, destinationSuffix);
	}

	private synchronized ServiceTracing getOrCreate(String name) {
		ServiceTracing s = map.get(name);
		if (s == null) {
			s = new ServiceTracing("EsbTemplate");
			map.put(name, s);
			statsService.registerService(name, s);
		}
		return s;
	}

	@Override
	public synchronized void destroy() throws Exception {
		for (String s : map.keySet()) {
			statsService.unregisterService(s);
		}
		map.clear();
	}

	@Override
	public void send(final EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		final long time = tracing.start();
		try {
			super.send(esbMessage);
		}
		finally {
			tracing.end(time, "send", () -> String.format("queue=%s, businessId='%s'", esbMessage.getServiceDestination(), esbMessage.getBusinessId()));
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
			tracing.end(time, "sendInternal", () -> String.format("queue=%s, businessId='%s'", esbMessage.getServiceDestination(), esbMessage.getBusinessId()));
		}
	}

	@Override
	public EsbMessage receive(final String destinationName) throws Exception {
		final ServiceTracing tracing = getOrCreate(destinationName);
		final long time = tracing.start();
		try {
			return super.receive(destinationName);
		}
		finally {
			tracing.end(time, "receive", () -> String.format("destinationName=%s", destinationName));
		}
	}

	@Override
	public EsbMessage receiveSelected(final String destinationName, final String messageSelector) throws Exception {
		final ServiceTracing tracing = getOrCreate(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveSelected(destinationName, messageSelector);
		}
		finally {
			tracing.end(time, "receiveSelected", () -> String.format("destinationName=%s, selector='%s'", destinationName, messageSelector));
		}
	}

	@Override
	public EsbMessage receiveInternal(final String destinationName) throws Exception {
		final ServiceTracing tracing = getOrCreate(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveInternal(destinationName);
		}
		finally {
			tracing.end(time, "receiveInternal", () -> String.format("destinatioName=%s", destinationName));
		}
	}

	@Override
	public EsbMessage receiveSelectedInternal(final String destinationName, final String messageSelector) throws Exception {
		final ServiceTracing tracing = getOrCreate(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveSelectedInternal(destinationName, messageSelector);
		}
		finally {
			tracing.end(time, "receiveSelectedInternal", () -> String.format("destinationName=%s, selector='%s'", destinationName, messageSelector));
		}
	}
}
