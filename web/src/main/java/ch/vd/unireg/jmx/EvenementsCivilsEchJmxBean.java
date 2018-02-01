package ch.vd.unireg.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

public interface EvenementsCivilsEchJmxBean {

	@ManagedAttribute(description = "Total number of events posted for processing since application start")
	int getNbMeaningfullEventsReceived();

	@ManagedAttribute(description = "Total number of individuals currently waiting to be processed")
	int getNbIndividualsAwaitingTreatment();
	
	@ManagedAttribute(description = "Total number of individuals currently waiting in the batch queue")
	int getNbIndividualsAwaitingInBatchQueue();

	@ManagedAttribute(description = "Total number of individuals currently waiting in the manual queue")
	int getNbIndividualsAwaitingInManualQueue();

	@ManagedAttribute(description = "Total number of individuals currently waiting in the immediate queue")
	int getNbIndividualsAwaitingInImmediateQueue();

	@ManagedAttribute(description = "Total number of individuals currently moving to the final queue")
	int getNbIndividualsMovingToFinalQueue();

	@ManagedAttribute(description = "Total number of individuals currently waiting in the final queue")
	int getNbIndividualsAwaitingInFinalQueue();

	@ManagedAttribute(description = "Average waiting time (ms) in the batch queue during the last 5 minutes")
	Long getSlidingAverageWaitingTimeInBatchQueue();

	@ManagedAttribute(description = "Average waiting time (ms) in the manual queue during the last 5 minutes")
	Long getSlidingAverageWaitingTimeInManualQueue();

	@ManagedAttribute(description = "Average waiting time (ms) in the immediate queue during the last 5 minutes")
	Long getSlidingAverageWaitingTimeInImmediateQueue();

	@ManagedAttribute(description = "Overall average waiting time (ms) in the batch queue since application start")
	Long getAverageWaitingTimeInBatchQueue();

	@ManagedAttribute(description = "Overall average waiting time (ms) in the manual queue since application start")
	Long getAverageWaitingTimeInManualQueue();

	@ManagedAttribute(description = "Overall average waiting time (ms) in the immediate queue since application start")
	Long getAverageWaitingTimeInImmediateQueue();

	@ManagedOperation(description = "Ask for (re-)treatment of the individual's event queue")
	@ManagedOperationParameters(value = {@ManagedOperationParameter(name = "idPerson", description = "ID of the individual whose events should be treated")})
	void treatPersonsEvents(long idPerson);

	@ManagedOperation(description = "Stops and restarts the processing thread")
	void restartProcessingThread();
}
