package ch.vd.uniregctb.wsclient.bvrplus;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClient;
import ch.vd.uniregctb.webservice.sipf.BVRPlusClientException;

public class BVRPlusClientTracing implements BVRPlusClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "BVRPlusClient";

	private BVRPlusClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(BVRPlusClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public BvrReponse getBVRDemande(BvrDemande bvrDemande) throws BVRPlusClientException {
		final long start = tracing.start();
		try {
			return target.getBVRDemande(bvrDemande);
		}
		finally {
			tracing.end(start, "getBVRDdemande");
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
