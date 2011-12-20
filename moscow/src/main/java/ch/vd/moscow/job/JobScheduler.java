package ch.vd.moscow.job;

import ch.vd.moscow.data.JobDefinition;
import org.quartz.SchedulerException;

import java.text.ParseException;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface JobScheduler {

	void registerJob(JobDefinition job) throws ParseException, SchedulerException;

	void updateJob(JobDefinition job) throws SchedulerException, ParseException;

	void unregisterJob(JobDefinition job) throws SchedulerException;

	void executeImmediately(JobDefinition job) throws SchedulerException;
}
