package ch.vd.uniregctb.jmx;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilAsyncProcessor;
import ch.vd.uniregctb.jms.ErrorMonitorableMessageListener;
import ch.vd.uniregctb.jms.JmxAwareEsbMessageEndpointManager;

@ManagedResource
public class EvenementsCivilsJmxBeanImpl implements EvenementsCivilsJmxBean, InitializingBean {

	private EvenementCivilAsyncProcessor evenementCivilAsyncProcessor;

	private JmxAwareEsbMessageEndpointManager evtCivilEndpointManager;

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
	public int getNbMeaningfullEventsReceived() {
		return evenementCivilAsyncProcessor.getNombreEvenementsRecus();
	}

	@Override
	@ManagedAttribute
	public int getNbEventsTreated() {
		return evenementCivilAsyncProcessor.getNombreEvenementsTraites();
	}

	@Override
	@ManagedAttribute
	public int getTreatmentQueueSize() {
		return evenementCivilAsyncProcessor.getQueueSize();
	}

	@Override
	@ManagedAttribute
	public int getNbConsumers() {
		return evtCivilEndpointManager.getMaxConcurrentConsumers();
	}

	@Override
	@ManagedAttribute
	public int getAcknowledgementDelay() {
		return evenementCivilAsyncProcessor.getDelaiPriseEnCompte();
	}

	@Override
	@ManagedAttribute
	public void setAcknowledgementDelay(int delay) {
		evenementCivilAsyncProcessor.setDelaiPriseEnCompte(delay);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilAsyncProcessor(EvenementCivilAsyncProcessor evenementCivilAsyncProcessor) {
		this.evenementCivilAsyncProcessor = evenementCivilAsyncProcessor;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilEndpointManager(JmxAwareEsbMessageEndpointManager evtCivilEndpointManager) {
		this.evtCivilEndpointManager = evtCivilEndpointManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (evtCivilEndpointManager == null) {
			throw new IllegalArgumentException("La propriété evtCivilEndpointManager est nulle");
		}

		final Object listener = evtCivilEndpointManager.getMessageListener();
		if (listener == null || !(listener instanceof ErrorMonitorableMessageListener)) {
			throw new IllegalArgumentException("Le listener d'événements civils doit implémenter l'interface " + ErrorMonitorableMessageListener.class.getName());
		}

		this.listener = (ErrorMonitorableMessageListener) listener;
	}
}
