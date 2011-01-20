package ch.vd.uniregctb.scheduler;

public class JobAlreadyStartedException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 3278685596263579943L;

	public JobAlreadyStartedException() {
		super("Job déjà démarré.");
	}

	public JobAlreadyStartedException(String message) {

		super(message);
	}

	public JobAlreadyStartedException(String message, Exception e) {

		super(e);
	}

}
