package ch.vd.uniregctb.ubr;

/**
 * Réponse du service /job/.../wait
 */
public class JobWaitInformation {

	/**
	 * Résultat de l'attente
	 */
	public enum JobWaitStatus {
		/**
		 * L'état attendu a été atteint par le job
		 */
		EXPECTED_STATUS_REACHED,

		/**
		 * Le timeout a été écoulé sans que je job atteigne l'état attendu
		 */
		TIMEOUT_OCCURRED
	}

	private JobWaitStatus waitStatus;
	private JobStatus jobStatus;
	private String jobRunningMessage;

	public JobWaitInformation() {
	}

	public JobWaitInformation(JobWaitStatus waitStatus, JobStatus jobStatus, String jobRunningMessage) {
		this.waitStatus = waitStatus;
		this.jobStatus = jobStatus;
		this.jobRunningMessage = jobRunningMessage;
	}

	public JobWaitStatus getWaitStatus() {
		return waitStatus;
	}

	public void setWaitStatus(JobWaitStatus waitStatus) {
		this.waitStatus = waitStatus;
	}

	public JobStatus getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(JobStatus jobStatus) {
		this.jobStatus = jobStatus;
	}

	public String getJobRunningMessage() {
		return jobRunningMessage;
	}

	public void setJobRunningMessage(String jobRunningMessage) {
		this.jobRunningMessage = jobRunningMessage;
	}
}
