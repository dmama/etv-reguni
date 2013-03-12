package ch.vd.uniregctb.jmx;

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

	@ManagedOperation(description = "Ask for (re-)treatment of the individual's event queue")
	@ManagedOperationParameters(value = {@ManagedOperationParameter(name = "idPerson", description = "ID of the individual whose events should be treated")})
	void treatPersonsEvents(long idPerson);

	@ManagedOperation(description = "Stops and restarts the processing thread")
	@ManagedOperationParameters(value = {@ManagedOperationParameter(name = "agressiveKill", description = "whether the thread should be interrupted or gently asked to stop")})
	void restartProcessingThread(boolean agressiveKill);
}
