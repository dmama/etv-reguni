package ch.vd.moscow.job.processor;

import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.job.JobStatus;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface JobProcessor<T extends JobDefinition> {

	/**
	 * Run the job immediately
	 *
	 * @param job    the job to run
	 * @param status the execution status
	 */
	void execute(T job, JobStatus status);
}
