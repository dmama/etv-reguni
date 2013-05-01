package ch.vd.unireg.interfaces.civil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.load.MethodCallDescriptor;

/**
 * Façade du service civil raw à utiliser pour exposer le service civil en spring remoting.
 * <p/>
 * Cette implémentation délègue tous les appels à une autre instance du service, en loggant et wrappant toutes les exceptions dans des {@link ServiceCivilException}.
 */
public class ServiceCivilEndPoint implements ServiceCivilRaw, ServiceCivilServiceWrapper, DetailedLoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilEndPoint.class);

	private ServiceCivilRaw target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(ServiceCivilRaw target) {
		this.target = target;
	}

	@Override
	public Individu getIndividu(long noIndividu, final AttributeIndividu... parties) throws ServiceCivilException {
		loadMeter.start(new MethodCallDescriptor("getIndividu", "noIndividu", noIndividu, "parties", parties));
		try {
			return target.getIndividu(noIndividu, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividu(noIndividu=" + noIndividu + ",parties=" + Arrays.toString(parties) + ") : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws ServiceCivilException {
		loadMeter.start(new MethodCallDescriptor("getIndividus", "nosIndividus", nosIndividus, "parties", parties));
		try {
			return target.getIndividus(nosIndividus, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividus(nosIndividu=" + Arrays.toString(nosIndividus.toArray()) + ",parties=" + Arrays.toString(parties) + ") : "
					+ getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {
		loadMeter.start(new MethodCallDescriptor("getIndividuByEvent", "eventId", evtId, "parties", parties));
		try {
			return target.getIndividuByEvent(evtId, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividuByEvent(evtId=" + evtId + ",parties=" + Arrays.toString(parties) + ") : "
					             + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
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
			throw new ServiceCivilException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Nationalite getNationaliteAt(long noIndividu, @Nullable RegDate date) {
		loadMeter.start(new MethodCallDescriptor("getNationaliteAt", "noIndividu", noIndividu, "date", date));
		try {
			return target.getNationaliteAt(noIndividu, date);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getNationaliteAt(noIndividu=" + noIndividu + ",date=" + date + ") : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public void ping() throws ServiceCivilException {
		loadMeter.start(new MethodCallDescriptor("ping"));
		try {
			target.ping();
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans ping() : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
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
			throw new ServiceCivilException(getMessage(e));
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
