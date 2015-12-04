package ch.vd.moscow.controller.job;

import ch.vd.moscow.job.JobStatus;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobStatusView {

	private Integer percentCompletion;
	private String message;

	public JobStatusView(JobStatus status) {
		this.percentCompletion = status.getPercentCompletion();
		this.message = status.getMessage();
	}

	public Integer getPercentCompletion() {
		return percentCompletion;
	}

	public String getMessage() {
		return message;
	}
}
