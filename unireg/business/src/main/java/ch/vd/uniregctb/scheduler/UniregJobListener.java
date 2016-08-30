package ch.vd.uniregctb.scheduler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniregJobListener implements JobListener {

	public static final UniregJobListener INSTANCE = new UniregJobListener();

	private static final String NAME = "JobListener";
	private static final Logger LOGGER = LoggerFactory.getLogger(UniregJobListener.class);

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		final JobDefinition job = getJobDefinition(context);
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + getName() + "> execution is VETOED");
		}
		job.interrupt();
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		final JobDefinition job = getJobDefinition(context);
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + getName() + "> is to be executed");
		}
		job.toBeExecuted();
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		final JobDefinition job = getJobDefinition(context);
		job.wasExecuted();
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + getName() + "> is now stopped with status " + job.getStatut());
		}
	}

	private static JobDefinition getJobDefinition(JobExecutionContext context) {
		return (JobDefinition) context.getMergedJobDataMap().get(JobDefinition.KEY_JOB);
	}
}
