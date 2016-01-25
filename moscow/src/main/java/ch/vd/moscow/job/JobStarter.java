package ch.vd.moscow.job;

import ch.vd.moscow.data.JobDefinition;
import org.apache.log4j.Logger;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;


public class JobStarter implements Job, InterruptableJob {

	private final Logger LOGGER = Logger.getLogger(JobStarter.class);

	private JobDefinition job;
	private JobManager manager;

	@Override
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext ctxt) throws JobExecutionException {

		final JobDataMap jobData = ctxt.getJobDetail().getJobDataMap();
		final JobDataMap triggerData = ctxt.getTrigger().getJobDataMap();

		job = (JobDefinition) jobData.get(JobDefinition.KEY_JOB);
		manager = (JobManager) jobData.get(JobDefinition.KEY_MANAGER);
		final String initialThreadName = Thread.currentThread().getName();

		try {
			// on donne le nom du job au thread d'exécution, de manière à le repérer plus facilement
			Thread.currentThread().setName(job.getName());

			manager.execute(job);
		}
		catch (Exception e) {
			LOGGER.error("Job <" + job.getName() + "> exception: " + e.getMessage(), e);
		}
		catch (Error e) {
			LOGGER.fatal("Job <" + job.getName() + "> error: " + e.getMessage(), e);
			throw e;
		}
		finally {
			Thread.currentThread().setName(initialThreadName);
		}
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		manager.interrupt(job);
	}
}
