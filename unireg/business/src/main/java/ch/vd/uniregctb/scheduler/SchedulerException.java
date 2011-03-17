package ch.vd.uniregctb.scheduler;

public class SchedulerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7438348448725638102L;

	public SchedulerException(String message) {
		super(message);
	}
	
	public SchedulerException(String message, Exception e) {
		super(message, e);
	}

}
