package ch.vd.uniregctb.jms;

import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;

import java.util.HashMap;
import java.util.Map;

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
				s = new ServiceTracing();
				map.put(destination, s);
				statsService.registerService(destination, s);
			}
			return s;
		}
	}

	public void destroy() throws Exception {
		for (String s : map.keySet()) {
			statsService.unregisterService(s);
		}
	}

	@Override
	public void send(EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		long time = tracing.start();
		try {
			super.send(esbMessage);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public void sendInternal(EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		long time = tracing.start();
		try {
			super.sendInternal(esbMessage);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public void sendError(EsbMessage esbMessage, String errorMessage, Exception exception, ErrorType errorType, String errorCode) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		long time = tracing.start();
		try {
			super.sendError(esbMessage, errorMessage, exception, errorType, errorCode);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receive(String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		long time = tracing.start();
		try {
			return super.receive(destinationName);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receiveSelected(String destinationName, String messageSelector) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		long time = tracing.start();
		try {
			return super.receiveSelected(destinationName, messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receiveInternal(String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		long time = tracing.start();
		try {
			return super.receiveInternal(destinationName);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receiveSelectedInternal(String destinationName, String messageSelector) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		long time = tracing.start();
		try {
			return super.receiveSelectedInternal(destinationName, messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}
}
