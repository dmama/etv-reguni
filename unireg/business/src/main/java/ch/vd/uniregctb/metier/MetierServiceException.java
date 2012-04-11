package ch.vd.uniregctb.metier;

/**
 * Exception levée dans le service métier.
 */
public class MetierServiceException extends Exception {

	public MetierServiceException() {
	}

	public MetierServiceException(String message) {
		super(message);
	}

	public MetierServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetierServiceException(Throwable cause) {
		super(cause);
	}
}
