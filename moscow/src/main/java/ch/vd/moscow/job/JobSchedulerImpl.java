package ch.vd.moscow.job;

import java.text.ParseException;
import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.database.DAO;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobSchedulerImpl implements JobScheduler, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(JobSchedulerImpl.class);

	private Scheduler scheduler;
	private JobManager manager;
	private DAO dao;
	private PlatformTransactionManager transactionManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(JobManager manager) {
		this.manager = manager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDao(DAO dao) {
		this.dao = dao;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
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
	public boolean updateJob(JobDefinition job) throws SchedulerException, ParseException {
		if (!scheduler.deleteJob(jobName(job), Scheduler.DEFAULT_GROUP)) {
			return false;
		}
		registerJob(job);
		return true;
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

	@Override
	public void afterPropertiesSet() throws Exception {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// register existing jobs at startup
				final List<JobDefinition> jobs = dao.getJobs();
				for (JobDefinition job : jobs) {
					try {
						LOGGER.debug("Registering job [" + job.getName() + "] with cron [" + job.getCronExpression() + "]");
						registerJob(job);
					}
					catch (Exception e) {
						LOGGER.error("Unable to register job [" + job.getName() + "] with cron [" + job.getCronExpression() + "]", e);
					}
				}
			}
		});
	}
}
