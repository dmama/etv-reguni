package ch.vd.unireg.interfaces.civil;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceCivilTracing implements ServiceCivilRaw, InitializingBean, DisposableBean, ServiceCivilServiceWrapper {

//	private static final Logger LOGGER = Logger.getLogger(ServiceCivilTracing.class);
	
	private ServiceCivilRaw target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceCivilRaw target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public Individu getIndividu(final long noIndividu, @Nullable final RegDate date, final AttributeIndividu... parties) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getIndividu(noIndividu, date, parties);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividu", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s, parties=%s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties));
				}
			});
		}
	}

	@Override
	public List<Individu> getIndividus(final Collection<Long> nosIndividus, @Nullable final RegDate date, final AttributeIndividu... parties) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Individu> list = target.getIndividus(nosIndividus, date, parties);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividus", items, new Object() {
				@Override
				public String toString() {
					return String.format("nosIndividus=%s, date=%s, parties=%s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date), ServiceTracing.toString(parties));
				}
			});
		}
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(final long eventId) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getIndividuFromEvent(eventId);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividuFromEvent", new Object() {
				@Override
				public String toString() {
					return String.format("eventId=%d", eventId);
				}
			});
		}
	}

	@Override
	public void ping() throws ServiceCivilException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ping", null);
		}
	}

	@Override
	public boolean isWarmable() {
		return target.isWarmable();
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
	public ServiceCivilRaw getTarget() {
		return target;
	}

	@Override
	public ServiceCivilRaw getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
