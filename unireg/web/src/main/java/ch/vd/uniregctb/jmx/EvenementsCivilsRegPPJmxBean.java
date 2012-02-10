package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

public interface EvenementsCivilsRegPPJmxBean extends EvenementsCivilsJmxBean {

	@ManagedAttribute(description = "Total number of events treated since application start")
	int getNbEventsTreated();

	@ManagedAttribute(description = "Number of events currently waiting to be processed sequencially")
	int getTreatmentQueueSize();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	int getAcknowledgementDelay();

	@ManagedAttribute(description = "Time to wait (seconds) before trying to sort incoming events")
	void setAcknowledgementDelay(int delay);

}
