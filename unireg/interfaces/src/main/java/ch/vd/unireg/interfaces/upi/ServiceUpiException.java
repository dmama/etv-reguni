package ch.vd.unireg.interfaces.upi;

/**
 * Exception renvoyée par le service UPI en cas de problème
 */
public class ServiceUpiException extends RuntimeException {

	private static final long serialVersionUID = -3721938805409540259L;

	public ServiceUpiException(String message) {
		super(message);
	}

	public ServiceUpiException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceUpiException(Throwable cause) {
		super(cause);
	}
}
