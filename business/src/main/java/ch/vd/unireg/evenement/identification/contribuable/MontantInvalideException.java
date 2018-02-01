package ch.vd.unireg.evenement.identification.contribuable;

public class MontantInvalideException extends Exception {

	private static final long serialVersionUID = -3017692966004788000L;

	public MontantInvalideException(Throwable throwable) {
		super(throwable);
	}

	public MontantInvalideException(String message) {
		super(message);
	}

	public MontantInvalideException(String message, Throwable cause) {
		super(message, cause);
	}
}