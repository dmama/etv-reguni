package ch.vd.uniregctb.jmx;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilAsyncProcessor;
import ch.vd.uniregctb.jms.ErrorMonitorableMessageListener;

@ManagedResource
public class EvenementsCivilsJmxBeanImpl implements EvenementsCivilsJmxBean, InitializingBean {

	private EvenementCivilAsyncProcessor evenementCivilAsyncProcessor;

	private DefaultMessageListenerContainer evtCivilListenerContainer;

	private ErrorMonitorableMessageListener listener;

	@ManagedAttribute
	public int getNbEventsReceived() {
		return listener.getNombreMessagesRecus();
	}

	@ManagedAttribute
	public int getNbEventsRejectedToErrorQueue() {
		return listener.getNombreMessagesRenvoyesEnErreur();
	}

	@ManagedAttribute
	public int getNbEventsRejectedException() {
		return listener.getNombreMessagesRenvoyesEnException();
	}

	@ManagedAttribute
	public int getNbMeaningfullEventsReceived() {
		return evenementCivilAsyncProcessor.getNombreEvenementsRecus();
	}

	@ManagedAttribute
	public int getNbEventsTreated() {
		return evenementCivilAsyncProcessor.getNombreEvenementsTraites();
	}

	@ManagedAttribute
	public int getTreatmentQueueSize() {
		return evenementCivilAsyncProcessor.getQueueSize();
	}

	@ManagedAttribute
	public int getNbConsumers() {
		return evtCivilListenerContainer.getConcurrentConsumers();
	}

	@ManagedAttribute
	public int getAcknowledgementDelay() {
		return evenementCivilAsyncProcessor.getDelaiPriseEnCompte();
	}

	@ManagedAttribute
	public void setAcknowledgementDelay(int delay) {
		evenementCivilAsyncProcessor.setDelaiPriseEnCompte(delay);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilAsyncProcessor(EvenementCivilAsyncProcessor evenementCivilAsyncProcessor) {
		this.evenementCivilAsyncProcessor = evenementCivilAsyncProcessor;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilListenerContainer(DefaultMessageListenerContainer evtCivilListenerContainer) {
		this.evtCivilListenerContainer = evtCivilListenerContainer;
	}

	public void afterPropertiesSet() throws Exception {
		if (evtCivilListenerContainer == null) {
			throw new IllegalArgumentException("La propriété evtCivilListenerContainer est nulle");
		}

		final Object listener = evtCivilListenerContainer.getMessageListener();
		if (listener == null || !(listener instanceof ErrorMonitorableMessageListener)) {
			throw new IllegalArgumentException("Le listener d'événements civils doit implémenter l'interface " + ErrorMonitorableMessageListener.class.getName());
		}

		this.listener = (ErrorMonitorableMessageListener) listener;
	}
}
