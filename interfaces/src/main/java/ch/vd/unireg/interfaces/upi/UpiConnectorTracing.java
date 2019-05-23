package ch.vd.unireg.interfaces.upi;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class UpiConnectorTracing implements UpiConnector, InitializingBean, DisposableBean {

	private UpiConnector target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(UpiConnector target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public UpiPersonInfo getPersonInfo(final String noAvs13) throws UpiConnectorException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final UpiPersonInfo ind = target.getPersonInfo(noAvs13);
			if (ind != null) {
				items = 1;
			}
			return ind;
		}
		catch (UpiConnectorException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonInfo", items, () -> String.format("noAvs13='%s'", noAvs13));
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}
}
