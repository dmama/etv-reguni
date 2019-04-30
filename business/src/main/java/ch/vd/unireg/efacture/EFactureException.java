package ch.vd.unireg.efacture;

public class EFactureException extends RuntimeException {

	public EFactureException(String message) {
		super(message);
	}

	public EFactureException(String message, Throwable cause) {
		super(message, cause);
	}

	public EFactureException(Throwable cause) {
		super(cause);
	}
}
