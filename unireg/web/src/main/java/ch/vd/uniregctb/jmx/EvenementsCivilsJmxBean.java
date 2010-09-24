package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

public interface EvenementsCivilsJmxBean {

	@ManagedAttribute(description = "Number of events currently waiting to be processed sequencially")
	int getTreatmentQueueSize();

	@ManagedAttribute(description = "Number of concurrent consumers listening to the ESB queue")
	int getConsumers();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	int getAcknowledgementDelay();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	void setAcknowledgementDelay(int delay);

}
