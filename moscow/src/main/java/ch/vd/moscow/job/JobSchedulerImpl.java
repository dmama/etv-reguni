package ch.vd.moscow.job;

import ch.vd.moscow.data.JobDefinition;
import org.quartz.*;

import java.text.ParseException;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobSchedulerImpl implements JobScheduler {

	private Scheduler scheduler;
	private JobManager manager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(JobManager manager) {
		this.manager = manager;
	}

	@Override
	public void registerJob(JobDefinition job) throws ParseException, SchedulerException {

		final Trigger trigger = new CronTrigger(cronTriggerName(job), Scheduler.DEFAULT_GROUP, job.getCronExpression());
		registerDetails(job);
		trigger.setJobName(jobName(job));
		trigger.setJobGroup(Scheduler.DEFAULT_GROUP);
		scheduler.scheduleJob(trigger);
	}

	@Override
	public void updateJob(JobDefinition job) throws SchedulerException, ParseException {
		scheduler.deleteJob(jobName(job), Scheduler.DEFAULT_GROUP);
		registerJob(job);
	}

	@Override
	public void unregisterJob(JobDefinition job) throws SchedulerException {
		scheduler.unscheduleJob(cronTriggerName(job), Scheduler.DEFAULT_GROUP);
	}

	@Override
	public void executeImmediately(JobDefinition job) throws SchedulerException {
		final Trigger trigger = new SimpleTrigger(immediateTriggerName(job), Scheduler.DEFAULT_GROUP);
		registerDetails(job);
		trigger.setJobName(jobName(job));
		trigger.setJobGroup(Scheduler.DEFAULT_GROUP);
		scheduler.scheduleJob(trigger);
	}

	private void registerDetails(JobDefinition job) throws SchedulerException {
		JobDetail jobDetail = scheduler.getJobDetail(jobName(job), Scheduler.DEFAULT_GROUP);
		if (jobDetail == null) {

			jobDetail = new JobDetail(jobName(job), Scheduler.DEFAULT_GROUP, JobStarter.class);
			jobDetail.setDurability(true); // garde les détails du job après exécution

			jobDetail.getJobDataMap().put(JobDefinition.KEY_JOB, job);
			jobDetail.getJobDataMap().put(JobDefinition.KEY_MANAGER, manager);
//			jobDetail.addJobListener(jobName(job));

			scheduler.addJob(jobDetail, false);
		}
	}

	private static String jobName(JobDefinition job) {
		return "Job" + job.getId();
	}

	private static String immediateTriggerName(JobDefinition job) {
		return "ImmediateTrigger-" + job.getId();
	}

	private static String cronTriggerName(JobDefinition job) {
		return "CronTrigger-" + job.getId();
	}
}
