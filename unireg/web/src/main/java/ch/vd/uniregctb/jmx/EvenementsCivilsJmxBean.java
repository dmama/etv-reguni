package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

public interface EvenementsCivilsJmxBean {

	@ManagedAttribute(description = "Total number of events received from the JMS queue")
	int getAllEventsReceived();

	@ManagedAttribute(description = "Total number of events posted for processing since application start")
	int getMeaningfullEventsReceived();

	@ManagedAttribute(description = "Total number of events treated since application start")
	int getEventsTreated();

	@ManagedAttribute(description = "Number of events currently waiting to be processed sequencially")
	int getTreatmentQueueSize();

	@ManagedAttribute(description = "Number of concurrent consumers listening to the ESB queue")
	int getConsumers();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	int getAcknowledgementDelay();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	void setAcknowledgementDelay(int delay);

}
