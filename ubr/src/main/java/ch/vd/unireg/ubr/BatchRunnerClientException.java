package ch.vd.unireg.ubr;

public class BatchRunnerClientException extends Exception {

	public BatchRunnerClientException() {
	}

	public BatchRunnerClientException(String message) {
		super(message);
	}

	public BatchRunnerClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public BatchRunnerClientException(Throwable cause) {
		super(cause);
	}

	public BatchRunnerClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
