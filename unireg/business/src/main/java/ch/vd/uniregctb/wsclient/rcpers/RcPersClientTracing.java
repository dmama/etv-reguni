package ch.vd.uniregctb.wsclient.rcpers;

import java.util.Collection;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class RcPersClientTracing implements RcPersClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "RcPersClient";

	private RcPersClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(RcPersClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public ListOfPersons getPersons(final Collection<Long> ids, final RegDate date, final boolean withHistory) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPersons(ids, date, withHistory);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersons", new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s, date=%s, withHistory=%s", ServiceTracing.toString(ids), ServiceTracing.toString(date), withHistory);
				}
			});
		}
	}

	@Override
	public Person getPersonForEvent(final long eventId) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPersonForEvent(eventId);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonForEvent", new Object() {
				@Override
				public String toString() {
					return String.format("eventId=%d", eventId);
				}
			});
		}
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
}
