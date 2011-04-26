package ch.vd.uniregctb.wsclient.acicom;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservice.acicom.AciComClient;
import ch.vd.uniregctb.webservice.acicom.AciComClientException;

public class AciComClientTracing implements AciComClient, InitializingBean, DisposableBean {

	public final String SERVICE_NAME = "AciComClient";

	private AciComClient target;

	private StatsService statsService;
	
	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(AciComClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public ContenuMessage recupererMessage(final RecupererContenuMessage infosMessage) throws AciComClientException {
		final long start = tracing.start();
		try {
			return target.recupererMessage(infosMessage);
		}
		finally {
			tracing.end(start, "recupererMessage", new Object() {
				@Override
				public String toString() {
					return String.format("infosMessage=%s", infosMessage != null ? infosMessage.getMessageId() : null);
				}
			});
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}
}
