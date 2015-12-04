package ch.vd.moscow.job;

import ch.vd.moscow.data.JobDefinition;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface JobManager {
	/**
	 * @param job a job definition
	 * @return the status if the job is running (unmodifiable); or <b>null</b> if the job is not running.
	 */
	JobStatus getStatus(JobDefinition job);

	void execute(JobDefinition job);

	void interrupt(JobDefinition job);
}
