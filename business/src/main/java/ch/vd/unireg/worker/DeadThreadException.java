package ch.vd.unireg.worker;

public class DeadThreadException extends Exception {

	private String threadName;

	public DeadThreadException() {
	}

	public DeadThreadException(String threadName) {
		this.threadName = threadName;
	}

	public String getThreadName() {
		return threadName;
	}
}
