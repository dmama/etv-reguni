package ch.vd.uniregctb.jmx;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.engine.EvenementCivilAsyncProcessor;
import ch.vd.uniregctb.evenement.jms.EvenementCivilListener;

@ManagedResource
public class EvenementsCivilsJmxBeanImpl implements EvenementsCivilsJmxBean {

	private EvenementCivilAsyncProcessor evenementCivilAsyncProcessor;

	private DefaultMessageListenerContainer evtCivilListenerContainer;

	@ManagedAttribute
	public int getAllEventsReceived() {
		return EvenementCivilListener.getNombreMessagesRecus();
	}

	@ManagedAttribute
	public int getMeaningfullEventsReceived() {
		return evenementCivilAsyncProcessor.getNombreEvenementsRecus();
	}

	@ManagedAttribute
	public int getEventsTreated() {
		return evenementCivilAsyncProcessor.getNombreEvenementsTraites();
	}

	@ManagedAttribute
	public int getTreatmentQueueSize() {
		return evenementCivilAsyncProcessor.getQueueSize();
	}

	@ManagedAttribute
	public int getConsumers() {
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

	public void setEvenementCivilAsyncProcessor(EvenementCivilAsyncProcessor evenementCivilAsyncProcessor) {
		this.evenementCivilAsyncProcessor = evenementCivilAsyncProcessor;
	}

	public void setEvtCivilListenerContainer(DefaultMessageListenerContainer evtCivilListenerContainer) {
		this.evtCivilListenerContainer = evtCivilListenerContainer;
	}
}
