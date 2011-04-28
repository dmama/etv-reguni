package ch.vd.uniregctb.scheduler;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class UniregJobListener implements JobListener {

	private static final Logger LOGGER = Logger.getLogger(UniregJobListener.class);

	private final JobDefinition job;

	public UniregJobListener(JobDefinition job) {
		this.job = job;
	}

	public String getName() {
		return job.getName();
	}

	public void jobExecutionVetoed(JobExecutionContext context) {
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + getName() + "> execution is VETOED");
		}
		job.interrupt();
	}

	public void jobToBeExecuted(JobExecutionContext context) {
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + getName() + "> is to be executed");
		}
		job.toBeExecuted();
	}

	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		job.wasExecuted();
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + getName() + "> is now stopped with status " + job.getStatut());
		}
	}

}
