package ch.vd.uniregctb.jms;

import javax.jms.MessageListener;

import org.apache.activemq.ra.MessageActivationSpec;
import org.apache.log4j.Logger;

import ch.vd.technical.esb.jms.EsbMessageEndpointManager;

/**
 * Classe qui se comporte comme un {@link EsbMessageEndpointManager} en offrant :
 * <ul>
 * <li>la possibilité d'exporter quelques valeurs d'attributs par JMX</li>
 * <li>permet de démarrer et arrêter l'écoute de la queue par JMX</li>
 * </ul>
 */
public class JmxAwareEsbMessageEndpointManager extends EsbMessageEndpointManager implements MessageEndpointManagerJmxInterface {

	private static final Logger LOGGER = Logger.getLogger(JmxAwareEsbMessageEndpointManager.class);

	private MessageListener messageListener;

	@Override
	public String getDestinationName() {
		return ((MessageActivationSpec)getActivationSpec()).getDestination();
	}

	@Override
	public int getMaxConcurrentConsumers() {
		return ((MessageActivationSpec)getActivationSpec()).getMaxSessionsIntValue();
	}

	@Override
	public void setMessageListener(final MessageListener messageListener) {
		if (!(messageListener instanceof MonitorableMessageListener)) {
			throw new IllegalArgumentException("Le listener doit implémenter l'interface " + MonitorableMessageListener.class.getName());
		}
		this.messageListener = messageListener;
		super.setMessageListener(messageListener);
	}

	@Override
	public void start() {
		super.start();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Ecoute sur la queue '%s' démarrée", getDestinationName()));
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Ecoute sur la queue '%s' arrêtée", getDestinationName()));
		}
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}

	@Override
	public int getReceivedMessages() {
		return ((MonitorableMessageListener)messageListener).getNombreMessagesRecus();
	}
}
