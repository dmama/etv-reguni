package ch.vd.moscow.job;

import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.job.processor.JobProcessor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobManagerImpl implements JobManager, ApplicationContextAware {

	private ApplicationContext context;

	private Map<Long, JobStatus> statuses = Collections.synchronizedMap(new HashMap<Long, JobStatus>());

	@Override
	public JobStatus getStatus(JobDefinition job) {
		final JobStatus status = statuses.get(job.getId());
		return status == null ? null : new InteractiveJobStatus(status);
	}

	@Override
	public void execute(JobDefinition job) {

		if (statuses.containsKey(job.getId())) {
			throw new IllegalArgumentException("Job #" + job.getId() + " is already running");
		}

		final Class<? extends JobProcessor> processorClass = job.getProcessorClass();
		final JobProcessor processor = context.getBean(processorClass);

		try {
			final JobStatus status = new InteractiveJobStatus();
			statuses.put(job.getId(), status);

			//noinspection unchecked
			processor.execute(job, status);
		}
		finally {
			statuses.remove(job.getId());
		}
	}

	@Override
	public void interrupt(JobDefinition job) {

		final JobStatus status = statuses.get(job.getId());
		if (status == null) {
			throw new IllegalArgumentException("Job #" + job.getId() + " is not running");
		}

		status.interrupt();
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}
