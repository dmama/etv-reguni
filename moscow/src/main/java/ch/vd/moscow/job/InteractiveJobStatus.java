package ch.vd.moscow.job;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class InteractiveJobStatus implements JobStatus {

	private boolean interrupted;
	private Integer percentCompletion;
	private String message;

	public InteractiveJobStatus() {
	}

	public InteractiveJobStatus(JobStatus right) {
		this.interrupted = right.isInterrupted();
		this.percentCompletion = right.getPercentCompletion();
		this.message = right.getMessage();
	}

	@Override
	public Integer getPercentCompletion() {
		return percentCompletion;
	}

	@Override
	public void setPercentCompletion(Integer percentCompletion) {
		this.percentCompletion = percentCompletion;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public boolean isInterrupted() {
		return interrupted;
	}

	@Override
	public void interrupt() {
		this.interrupted = true;
	}
}
