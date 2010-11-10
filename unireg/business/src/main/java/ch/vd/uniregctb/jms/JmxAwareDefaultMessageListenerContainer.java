package ch.vd.uniregctb.jms;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Classe qui se comporte exactement comme un {@link org.springframework.jms.listener.DefaultMessageListenerContainer}
 * en offrant la possibilité d'exporter quelques valeurs d'attributs par JMX (et surtout qui loggue les démarrages et les arrêts)
 */
public class JmxAwareDefaultMessageListenerContainer extends DefaultMessageListenerContainer implements MessageListenerContainerJmxInterface {

	private static final Logger LOGGER = Logger.getLogger(JmxAwareDefaultMessageListenerContainer.class);

	@Override
	public void doStart() throws JMSException {
		super.doStart();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Ecoute sur la queue '%s' démarrée", getDestinationName()));
		}
	}

	@Override
	public void doStop() throws JMSException {
		super.doStop();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Ecoute sur la queue '%s' arrêtée", getDestinationName()));
		}
	}
}
