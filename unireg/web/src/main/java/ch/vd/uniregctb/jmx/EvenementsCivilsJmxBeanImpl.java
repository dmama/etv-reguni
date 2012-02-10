package ch.vd.uniregctb.jmx;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import ch.vd.uniregctb.jms.ErrorMonitorableMessageListener;
import ch.vd.uniregctb.jms.JmxAwareEsbMessageEndpointManager;

public abstract class EvenementsCivilsJmxBeanImpl implements EvenementsCivilsJmxBean, InitializingBean {

	private JmxAwareEsbMessageEndpointManager endpointManager;

	private ErrorMonitorableMessageListener listener;

	@Override
	@ManagedAttribute
	public int getNbEventsReceived() {
		return listener.getNombreMessagesRecus();
	}

	@Override
	@ManagedAttribute
	public int getNbEventsRejectedToErrorQueue() {
		return listener.getNombreMessagesRenvoyesEnErreur();
	}

	@Override
	@ManagedAttribute
	public int getNbEventsRejectedException() {
		return listener.getNombreMessagesRenvoyesEnException();
	}

	@Override
	@ManagedAttribute
	public int getNbConsumers() {
		return endpointManager.getMaxConcurrentConsumers();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEndpointManager(JmxAwareEsbMessageEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (endpointManager == null) {
			throw new IllegalArgumentException("La propriété endpointManager est nulle");
		}

		final Object listener = endpointManager.getMessageListener();
		if (listener == null || !(listener instanceof ErrorMonitorableMessageListener)) {
			throw new IllegalArgumentException("Le listener d'événements civils doit implémenter l'interface " + ErrorMonitorableMessageListener.class.getName());
		}

		this.listener = (ErrorMonitorableMessageListener) listener;
	}
}
