package ch.vd.unireg.data;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class CivilDataEventNotifierTracing implements CivilDataEventNotifier, InitializingBean, DisposableBean {

	private CivilDataEventNotifier target;
	private StatsService statsService;
	private String serviceName;

	private ServiceTracing tracing;

	public void setTarget(CivilDataEventNotifier target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		tracing = new ServiceTracing(serviceName, true);
		if (statsService != null) {
			statsService.registerService(serviceName, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(serviceName);
		}
	}

	@Override
	public void notifyEntrepriseChange(final long id) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.notifyEntrepriseChange(id);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "notifyEntrepriseChange", () -> String.format("id=%d", id));
		}
	}

	@Override
	public void notifyIndividuChange(long id) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.notifyIndividuChange(id);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "notifyIndividuChange", () -> String.format("id=%d", id));
		}
	}
}
