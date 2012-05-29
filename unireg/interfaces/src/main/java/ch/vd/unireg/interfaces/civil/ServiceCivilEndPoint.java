package ch.vd.unireg.interfaces.civil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;

/**
 * Façade du service civil raw à utiliser pour exposer le service civil en spring remoting.
 * <p/>
 * Cette implémentation délègue tous les appels à une autre instance du service, en loggant et wrappant toutes les exceptions dans des {@link ServiceCivilException}.
 */
public class ServiceCivilEndPoint implements ServiceCivilRaw {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilEndPoint.class);

	private ServiceCivilRaw target;

	public void setTarget(ServiceCivilRaw target) {
		this.target = target;
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
		try {
			return target.getIndividu(noIndividu, date, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividu(noIndividu=" + noIndividu + ",date=" + date + ",parties=" + Arrays.toString(parties) + ") : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties) {
		try {
			return target.getIndividus(nosIndividus, date, parties);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividus(nosIndividu=" + Arrays.toString(nosIndividus.toArray()) + ",date=" + date + ",parties=" + Arrays.toString(parties) + ") : "
					+ getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		try {
			return target.getIndividuFromEvent(eventId);
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans getIndividuFromEvent(eventId=" + eventId + ") : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
	}

	@Override
	public boolean isWarmable() {
		try {
			return target.isWarmable();
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception dans isWarmable() : " + getMessage(e), e);
			// on ne transmet que le message, pour éviter de transmettre des éléments non-sérializable
			throw new ServiceCivilException(getMessage(e));
		}
	}

	private static String getMessage(RuntimeException e) {
		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.getClass().getSimpleName();
		}
		return message;
	}
}
