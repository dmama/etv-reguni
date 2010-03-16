package ch.vd.uniregctb.scheduler;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import ch.vd.uniregctb.scheduler.JobDefinition.JobStatut;

public class UniregJobListener implements JobListener {

	private static final Logger LOGGER = Logger.getLogger(UniregJobListener.class);

	private JobDefinition job;
	
	public UniregJobListener(JobDefinition job) {
		
		this.job = job;
	}
	
	public String getName() {
		return job.getName();
	}

	public void jobExecutionVetoed(JobExecutionContext context) {
		LOGGER.info("Listener: Job "+getName()+" was VETOED");
		job.setStatut(JobStatut.JOB_INTERRUPTED);
	}

	public void jobToBeExecuted(JobExecutionContext context) {
		LOGGER.info("Listener: Job "+getName()+" is now started");
		job.setStatut(JobStatut.JOB_RUNNING);
	}

	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		LOGGER.info("Listener: Job "+getName()+" is now stopped");
	}

}
