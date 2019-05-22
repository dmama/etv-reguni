package ch.vd.unireg.interfaces.upi;

/**
 * Exception renvoyée par le connecteur UPI en cas de problème
 */
public class UpiConnectorException extends RuntimeException {

	private static final long serialVersionUID = -3721938805409540259L;

	public UpiConnectorException(String message) {
		super(message);
	}

	public UpiConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

	public UpiConnectorException(Throwable cause) {
		super(cause);
	}
}
