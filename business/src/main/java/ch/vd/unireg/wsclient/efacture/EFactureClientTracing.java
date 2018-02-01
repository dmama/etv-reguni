package ch.vd.unireg.wsclient.efacture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class EFactureClientTracing implements EFactureClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "EFactureClient";

	private EFactureClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(EFactureClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public PayerWithHistory getHistory(final long payerBusinessId, final String billerId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final PayerWithHistory histo = target.getHistory(payerBusinessId, billerId);
			if (histo != null) {
				items = 1;
			}
			return histo;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getHistory", items, () -> String.format("payerBusinessId=%d, billerId=%s", payerBusinessId, billerId));
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}
}
