package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;

public interface EvenementsCivilsEchJmxBean {

	@ManagedAttribute(description = "Total number of events received from the JMS queue (manual events)")
	int getNbManualEventsReceived();

	@ManagedAttribute(description = "Total number of events received from the JMS queue (batch events)")
	int getNbBatchEventsReceived();

	@ManagedAttribute(description = "Total number of events sent to the error queue (manual events)")
	int getNbManualEventsRejectedToErrorQueue();

	@ManagedAttribute(description = "Total number of events sent to the error queue (batch events)")
	int getNbBatchEventsRejectedToErrorQueue();

	@ManagedAttribute(description = "Total number of rejected events (DLQ for manual events)")
	int getNbManualEventsRejectedException();

	@ManagedAttribute(description = "Total number of rejected events (DLQ for batch events)")
	int getNbBatchEventsRejectedException();

	@ManagedAttribute(description = "Total number of events posted for processing since application start")
	int getNbMeaningfullEventsReceived();

	@ManagedAttribute(description = "Number of concurrent consumers listening to the ESB queue (manual events)")
	int getNbManualConsumers();

	@ManagedAttribute(description = "Number of concurrent consumers listening to the ESB queue (batch events)")
	int getNbBatchConsumers();

	@ManagedAttribute(description = "Total number of individuals currently waiting to be processed")
	int getNbIndividualsAwaitingTreatment();

}
