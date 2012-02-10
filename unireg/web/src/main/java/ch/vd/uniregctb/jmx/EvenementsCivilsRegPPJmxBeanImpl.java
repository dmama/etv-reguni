package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilAsyncProcessor;

@ManagedResource
public class EvenementsCivilsRegPPJmxBeanImpl extends EvenementsCivilsJmxBeanImpl implements EvenementsCivilsRegPPJmxBean {

	private EvenementCivilAsyncProcessor evenementCivilAsyncProcessor;

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
}
