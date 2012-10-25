package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

public interface EvenementsCivilsRegPPJmxBean {

	@ManagedAttribute(description = "Total number of events received from the JMS queue")
	int getNbEventsReceived();

	@ManagedAttribute(description = "Total number of events sent to the error queue")
	int getNbEventsRejectedToErrorQueue();

	@ManagedAttribute(description = "Total number of rejected events (DLQ)")
	int getNbEventsRejectedException();

	@ManagedAttribute(description = "Total number of events posted for processing since application start")
	int getNbMeaningfullEventsReceived();

	@ManagedAttribute(description = "Number of concurrent consumers listening to the ESB queue")
	int getNbConsumers();

	@ManagedAttribute(description = "Total number of events treated since application start")
	int getNbEventsTreated();

	@ManagedAttribute(description = "Number of events currently waiting to be processed sequencially")
	int getTreatmentQueueSize();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	int getAcknowledgementDelay();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	void setAcknowledgementDelay(int delay);

}
