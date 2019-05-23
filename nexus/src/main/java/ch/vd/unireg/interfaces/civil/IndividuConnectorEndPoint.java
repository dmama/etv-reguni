package ch.vd.unireg.interfaces.civil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.load.MethodCallDescriptor;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

/**
 * Façade à utiliser pour exposer le connecteur civil en spring remoting.
 * <p/>
 * Cette implémentation délègue tous les appels à une autre instance du service, en loggant et wrappant toutes les exceptions dans des {@link IndividuConnectorException}.
 */
public class IndividuConnectorEndPoint implements IndividuConnector, IndividuConnectorWrapper, DetailedLoadMonitorable {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividuConnectorEndPoint.class);

	private IndividuConnector target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(IndividuConnector target) {
		this.target = target;
	}

	@Override
	public Individu getIndividu(long noIndividu, final AttributeIndividu... parties) throws IndividuConnectorException {
		loadMeter.start(new MethodCallDescriptor("getIndividu", "noIndividu", noIndividu, "parties", parties));
		try {
			return target.getIndividu(noIndividu, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividu(noIndividu=" + noIndividu + ",parties=" + Arrays.toString(parties) + ") : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new IndividuConnectorException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws IndividuConnectorException {
		loadMeter.start(new MethodCallDescriptor("getIndividus", "nosIndividus", nosIndividus, "parties", parties));
		try {
			return target.getIndividus(nosIndividus, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividus(nosIndividu=" + Arrays.toString(nosIndividus.toArray()) + ",parties=" + Arrays.toString(parties) + ") : "
					+ getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new IndividuConnectorException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws IndividuConnectorException {
		loadMeter.start(new MethodCallDescriptor("getIndividuByEvent", "eventId", evtId, "parties", parties));
		try {
			return target.getIndividuByEvent(evtId, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividuByEvent(evtId=" + evtId + ",parties=" + Arrays.toString(parties) + ") : "
					             + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new IndividuConnectorException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
		loadMeter.start(new MethodCallDescriptor("getIndividuAfterEvent", "eventId", eventId));
		try {
			return target.getIndividuAfterEvent(eventId);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividuAfterEvent(eventId=" + eventId + ") : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new IndividuConnectorException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public void ping() throws IndividuConnectorException {
		loadMeter.start(new MethodCallDescriptor("ping"));
		try {
			target.ping();
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans ping() : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new IndividuConnectorException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public boolean isWarmable() {
		loadMeter.start(new MethodCallDescriptor("isWarmable"));
		try {
			return target.isWarmable();
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans isWarmable() : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new IndividuConnectorException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	private static String getMessage(RuntimeException e) {
		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.getClass().getSimpleName();
		}
		return message;
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
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
