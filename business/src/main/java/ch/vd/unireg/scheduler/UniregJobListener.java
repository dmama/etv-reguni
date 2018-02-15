package ch.vd.unireg.scheduler;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniregJobListener implements JobListener {

	public static final String NAME = "UniregJobListener";
	private static final Logger LOGGER = LoggerFactory.getLogger(UniregJobListener.class);

	@Nullable
	private final Consumer<JobDefinition> onJobExecutionEndListener;

	public UniregJobListener(@Nullable Consumer<JobDefinition> onJobExecutionEndListener) {
		this.onJobExecutionEndListener = onJobExecutionEndListener;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		final JobDefinition job = getJobDefinition(context);
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + job.getName() + "> execution is VETOED");
		}
		job.interrupt();
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		final JobDefinition job = getJobDefinition(context);
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + job.getName() + "> is to be executed");
		}
		job.toBeExecuted();
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		final JobDefinition job = getJobDefinition(context);
		job.wasExecuted();
		if (!job.isLogDisabled()) {
			LOGGER.info("Job <" + job.getName() + "> is now stopped with status " + job.getStatut());
		}

		if (onJobExecutionEndListener != null) {
			onJobExecutionEndListener.accept(job);
		}
	}

	private static JobDefinition getJobDefinition(JobExecutionContext context) {
		return (JobDefinition) context.getMergedJobDataMap().get(JobDefinition.KEY_JOB);
	}
}
