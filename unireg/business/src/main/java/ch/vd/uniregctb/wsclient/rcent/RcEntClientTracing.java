package ch.vd.uniregctb.wsclient.rcent;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0022.v1.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class RcEntClientTracing implements RcEntClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "RcEntClient";

	private RcEntClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(RcEntClient target) {
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
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	@Override
	public OrganisationData getOrganisation(final long id, final RegDate referenceDate, final boolean withHistory) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final OrganisationData data = target.getOrganisation(id, referenceDate, withHistory);
			if (data != null) {
				items = 1;
			}
			return data;
		}
		catch (RcEntClientException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisation", items, new Object() {
				@Override
				public String toString() {
					return String.format("id=%d, referenceDate=%s, history=%s", id, ServiceTracing.toString(referenceDate), withHistory);
				}
			});
		}
	}

	@Override
	public OrganisationsOfNotice getOrganisationsOfNotice(final long noticeId, final OrganisationState when) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final OrganisationsOfNotice data = target.getOrganisationsOfNotice(noticeId, when);
			if (data != null) {
				items = 1;
			}
			return data;
		}
		catch (RcEntClientException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationOfNotice", items, new Object() {
				@Override
				public String toString() {
					return String.format("noticeId=%d, when=%s", noticeId, when);
				}
			});
		}
	}

	@Override
	public void ping() throws RcEntClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (RcEntClientException e) {
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
