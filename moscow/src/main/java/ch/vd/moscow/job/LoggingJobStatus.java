package ch.vd.moscow.job;

import org.apache.log4j.Logger;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class LoggingJobStatus implements JobStatus {

	private static final Logger LOGGER = Logger.getLogger(LoggingJobStatus.class);

	@Override
	public Integer getPercentCompletion() {
		return null;
	}

	@Override
	public void setPercentCompletion(Integer percentCompletion) {
	}

	@Override
	public String getMessage() {
		return null;
	}

	@Override
	public void setMessage(String message) {
		LOGGER.debug(message);
	}

	@Override
	public boolean isInterrupted() {
		return false;
	}

	@Override
	public void interrupt() {
	}
}
