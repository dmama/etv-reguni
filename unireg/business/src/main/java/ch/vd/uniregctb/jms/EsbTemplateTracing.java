package ch.vd.uniregctb.jms;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.spring.EsbTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;

import javax.jms.Destination;
import java.util.HashMap;
import java.util.Map;

/**
 * Version spécialisée du template Esb qui relève les temps d'exécution des méthodes publiques.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EsbTemplateTracing extends EsbTemplate implements DisposableBean {

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
				statsService.registerRaw(destination, s);
			}
			return s;
		}
	}

	public void destroy() throws Exception {
		for (String s : map.keySet()) {
			statsService.unregisterRaw(s);
		}
	}

	@Override
	public void sendEsbMessage(String destinationName, EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		long time = tracing.start();
		try {
			super.sendEsbMessage(destinationName, esbMessage);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public void sendEsbMessage(Destination destination, EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		long time = tracing.start();
		try {
			super.sendEsbMessage(destination, esbMessage);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public void sendEsbMessage(EsbMessage esbMessage) throws Exception {
		final ServiceTracing tracing = get(esbMessage);
		long time = tracing.start();
		try {
			super.sendEsbMessage(esbMessage);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receiveEsbMessage() throws Exception {
		final ServiceTracing tracing = get(getDefaultDestinationName());
		long time = System.nanoTime();
		try {
			return super.receiveEsbMessage();
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receiveEsbMessage(String destinationName) throws Exception {
		final ServiceTracing tracing = get(destinationName);
		long time = tracing.start();
		try {
			return super.receiveEsbMessage(destinationName);
		}
		finally {
			tracing.end(time);
		}
	}

	@Override
	public EsbMessage receiveEsbMessage(Destination destination) throws Exception {
		final ServiceTracing tracing = get(destination.toString());
		long time = tracing.start();
		try {
			return super.receiveEsbMessage(destination);
		}
		finally {
			tracing.end(time);
		}
	}
}
