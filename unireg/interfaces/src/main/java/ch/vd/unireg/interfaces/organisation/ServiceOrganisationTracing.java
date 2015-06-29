package ch.vd.unireg.interfaces.organisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 */
public class ServiceOrganisationTracing implements ServiceOrganisationRaw, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOrganisationTracing.class);
	
	private ServiceOrganisationRaw target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
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

	@Override
	public Organisation getOrganisationHistory(final long noOrganisation) throws ServiceOrganisationException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Organisation organisation = target.getOrganisationHistory(noOrganisation);
			if (organisation != null) {
				items = 1;
			}
			return organisation;
		}
		catch (ServiceCivilException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationHistory", items, new Object() {
				@Override
				public String toString() {
					return String.format("noOrganisation=%d", noOrganisation);
				}
			});
		}
	}

	@Override
	public Long getOrganisationPourSite(final Long noSite) throws ServiceOrganisationException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Long noOrganisation = target.getOrganisationPourSite(noSite);
			if (noOrganisation != null) {
				items = 1;
			}
			return noOrganisation;
		}
		catch (ServiceCivilException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationPourSite", items, new Object() {
				@Override
				public String toString() {
					return String.format("noSite=%d", noSite);
				}
			});
		}
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (ServiceCivilException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ping", null);
		}
	}
}
