package ch.vd.unireg.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

public interface EvenementsOrganisationJmxBean {

	@ManagedAttribute(description = "Total number of events posted for processing since application start")
	int getNbMeaningfullEventsReceived();

	@ManagedAttribute(description = "Total number of organisations currently waiting to be processed")
	int getNbOrganisationsAwaitingTreatment();
	
	@ManagedAttribute(description = "Total number of organisations currently waiting in the batch queue")
	int getNbOrganisationsAwaitingInBatchQueue();

	@ManagedAttribute(description = "Total number of organisations currently waiting in the priority queue")
	int getNbOrganisationsAwaitingInPriorityQueue();

	@ManagedAttribute(description = "Total number of organisations currently waiting in the immediate queue")
	int getNbOrganisationsAwaitingInImmediateQueue();

	@ManagedAttribute(description = "Total number of organisations currently moving to the final queue")
	int getNbOrganisationsMovingToFinalQueue();

	@ManagedAttribute(description = "Total number of organisations currently waiting in the final queue")
	int getNbOrganisationsAwaitingInFinalQueue();

	@ManagedAttribute(description = "Average waiting time (ms) in the batch queue during the last 5 minutes")
	Long getSlidingAverageWaitingTimeInBatchQueue();

	@ManagedAttribute(description = "Average waiting time (ms) in the priority queue during the last 5 minutes")
	Long getSlidingAverageWaitingTimeInPriorityQueue();

	@ManagedAttribute(description = "Average waiting time (ms) in the immediate queue during the last 5 minutes")
	Long getSlidingAverageWaitingTimeInImmediateQueue();

	@ManagedAttribute(description = "Overall average waiting time (ms) in the batch queue since application start")
	Long getAverageWaitingTimeInBatchQueue();

	@ManagedAttribute(description = "Overall average waiting time (ms) in the priority queue since application start")
	Long getAverageWaitingTimeInPriorityQueue();

	@ManagedAttribute(description = "Overall average waiting time (ms) in the immediate queue since application start")
	Long getAverageWaitingTimeInImmediateQueue();

	@ManagedOperation(description = "Ask for (re-)treatment of the organisation's event queue")
	@ManagedOperationParameters(value = {@ManagedOperationParameter(name = "idOrganisation", description = "ID of the organisation whose events should be treated")})
	void treatOrganisationEvents(long idPerson);

	@ManagedOperation(description = "Stops and restarts the processing thread")
	void restartProcessingThread();

	@ManagedAttribute(description = "Capping level used during event processing (allowed values are null, A_VERIFIER and EN_ERREUR)")
	String getProcessingCappingLevel();

	@ManagedAttribute(description = "Capping level used during event processing (allowed values are null, A_VERIFIER and EN_ERREUR)")
	void setProcessingCappingLevel(String level);
}
