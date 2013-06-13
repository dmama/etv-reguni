package ch.vd.moscow.job;

import java.text.ParseException;

import org.quartz.SchedulerException;

import ch.vd.moscow.data.JobDefinition;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface JobScheduler {

	void registerJob(JobDefinition job) throws ParseException, SchedulerException;

	/**
	 * Update the job definition et re-register the cron trigger.
	 *
	 * @param job the job to update
	 * @return <b>true</b> if the job was successfully updated; <b>false</b> if the job was not registered in the first time.
	 * @throws SchedulerException in case of exception within the scheduler
	 * @throws ParseException     in case of bad cron format
	 */
	boolean updateJob(JobDefinition job) throws SchedulerException, ParseException;

	void unregisterJob(JobDefinition job) throws SchedulerException;

	void executeImmediately(JobDefinition job) throws SchedulerException;
}
