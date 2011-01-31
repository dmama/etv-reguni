package ch.vd.uniregctb.jms;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import ch.vd.technical.esb.jms.EsbMessageListenerContainer;

/**
 * Classe qui se comporte comme un {@link org.springframework.jms.listener.DefaultMessageListenerContainer} en offrant :
 * <ul>
 * <li>la possibilité d'exporter quelques valeurs d'attributs par JMX</li>
 * <li>permet de démarrer et arrêter l'écoute de la queue par JMX</li>
 * <li>attend que le context Spring soit entièrement (= tous les beans) initialisé pour commencer à écouter la queue</li>
 * </ul>
 */
public class JmxAwareDefaultMessageListenerContainer extends EsbMessageListenerContainer implements MessageListenerContainerJmxInterface, ApplicationListener {

	private static final Logger LOGGER = Logger.getLogger(JmxAwareDefaultMessageListenerContainer.class);

	private boolean wantAutoStartup = true;

	public JmxAwareDefaultMessageListenerContainer() {
		super.setAutoStartup(false); // on va gérer ça à la main lorsque le context est entièrement initialisé
	}

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

	@Override
	public void setMessageListener(Object messageListener) {
		if (messageListener instanceof MonitorableMessageListener) {
			super.setMessageListener(messageListener);
		}
		else {
			throw new IllegalArgumentException("Le listener doit implémenter l'interface " + MonitorableMessageListener.class.getName());
		}
	}

	public int getReceivedMessages() {
		final MonitorableMessageListener listener = (MonitorableMessageListener) getMessageListener();
		return listener.getNombreMessagesRecus();
	}

	@Override
	public void setAutoStartup(boolean autoStartup) {
		this.wantAutoStartup = autoStartup;
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (wantAutoStartup && !isRunning() && event instanceof ContextRefreshedEvent) {
			start();
		}
	}
}
