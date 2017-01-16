package ch.vd.uniregctb.data;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class CivilDataEventTracing implements CivilDataEventListener, InitializingBean, DisposableBean {

	private CivilDataEventListener target;
	private StatsService statsService;
	private String serviceName;

	private ServiceTracing tracing;

	public void setTarget(CivilDataEventListener target) {
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
	public void onOrganisationChange(final long id) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.onOrganisationChange(id);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "onOrganisationChange", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}
	}

	@Override
	public void onIndividuChange(long id) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.onIndividuChange(id);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "onIndividuChange", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}
	}
}
