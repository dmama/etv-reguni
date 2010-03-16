package ch.vd.vuta;

public class SmsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8439190015422180370L;

	public SmsException(String message, Exception e) {
		super(message, e);
	}

	public SmsException(String message) {
		super(message);
	}
}
