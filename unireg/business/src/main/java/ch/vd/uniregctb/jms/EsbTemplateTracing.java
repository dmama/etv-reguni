package ch.vd.uniregctb.jms;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import ch.vd.technical.esb.ErrorType;
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

	private final Map<String, ServiceTracing> map = new HashMap<String, ServiceTracing>();

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
	public void sendError(final EsbMessage esbMessage, final String errorMessage, Exception exception, ErrorType errorType, String errorCode) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		final long time = tracing.start();
		try {
			super.sendError(esbMessage, errorMessage, exception, errorType, errorCode);
		}
		finally {
			tracing.end(time, "sendError", new Object() {
				@Override
				public String toString() {
					return String.format("queue=%s, businessId='%s', errorMessage='%s'", esbMessage.getServiceDestination(), esbMessage.getBusinessId(), StringUtils.abbreviate(errorMessage, 100));
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

	@Override
	public EsbMessage receiveError(final String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveError(destinationName);
		}
		finally {
			tracing.end(time, "receiveError", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s", destinationName);
				}
			});
		}
	}

	@Override
	public EsbMessage receiveErrorInternal(final String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveErrorInternal(destinationName);
		}
		finally {
			tracing.end(time, "receiveErrorInternal", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s", destinationName);
				}
			});
		}
	}

	@Override
	public EsbMessage receiveSelectedError(final String destinationName, final String selector) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveSelectedError(destinationName, selector);
		}
		finally {
			tracing.end(time, "receiveSelectedError", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s, selector='%s'", destinationName, selector);
				}
			});
		}
	}

	@Override
	public EsbMessage receiveSelectedErrorInternal(final String destinationName, final String selector) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		final long time = tracing.start();
		try {
			return super.receiveSelectedErrorInternal(destinationName, selector);
		}
		finally {
			tracing.end(time, "receiveSelectedErrorInternal", new Object() {
				@Override
				public String toString() {
					return String.format("destinationName=%s, selector='%s'", destinationName, selector);
				}
			});
		}
	}
}
