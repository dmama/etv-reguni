package ch.vd.unireg.interfaces.civil;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du connecteur.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class IndividuConnectorTracing implements IndividuConnector, InitializingBean, DisposableBean, IndividuConnectorWrapper {

//	private static final Logger LOGGER = LoggerFactory.getLogger(IndividuConnectorTracing.class);
	
	private IndividuConnector target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(IndividuConnector target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public Individu getIndividu(final long noIndividu, final AttributeIndividu... parties) throws IndividuConnectorException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Individu ind = target.getIndividu(noIndividu, parties);
			if (ind != null) {
				items = 1;
			}
			return ind;
		}
		catch (IndividuConnectorException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividu", items, () -> String.format("noIndividu=%d, parties=%s", noIndividu, ServiceTracing.toString(parties)));
		}
	}

	@Override
	public List<Individu> getIndividus(final Collection<Long> nosIndividus, final AttributeIndividu... parties) throws IndividuConnectorException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Individu> list = target.getIndividus(nosIndividus, parties);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (IndividuConnectorException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividus", items, () -> String.format("nosIndividus=%s, parties=%s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(parties)));
		}
	}

	@Override
	public Individu getIndividuByEvent(final long evtId, final AttributeIndividu... parties) throws IndividuConnectorException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Individu ind = target.getIndividuByEvent(evtId, parties);
			if (ind != null) {
				items = 1;
			}
			return ind;
		}
		catch (IndividuConnectorException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividuByEvent", items, () -> String.format("evtId=%d, parties=%s", evtId, ServiceTracing.toString(parties)));
		}
	}

	@Override
	public IndividuApresEvenement getIndividuAfterEvent(final long eventId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final IndividuApresEvenement ind = target.getIndividuAfterEvent(eventId);
			if (ind != null) {
				items = 1;
			}
			return ind;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividuAfterEvent", items, () -> String.format("eventId=%d", eventId));
		}
	}

	@Override
	public void ping() throws IndividuConnectorException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (IndividuConnectorException e) {
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
	public IndividuConnector getTarget() {
		return target;
	}

	@Override
	public IndividuConnector getUltimateTarget() {
		if (target instanceof IndividuConnectorWrapper) {
			return ((IndividuConnectorWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
