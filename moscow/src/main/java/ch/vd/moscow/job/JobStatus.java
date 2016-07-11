package ch.vd.moscow.job;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface JobStatus {
	Integer getPercentCompletion();

	void setPercentCompletion(Integer percentCompletion);

	String getMessage();

	void setMessage(String message);

	boolean isInterrupted();

	void interrupt();
}
