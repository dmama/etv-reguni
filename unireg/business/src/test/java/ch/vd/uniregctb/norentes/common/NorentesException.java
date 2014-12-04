package ch.vd.uniregctb.norentes.common;

public class NorentesException extends RuntimeException {

	private static final long serialVersionUID = 1686948229881002335L;

	public NorentesException() {
		super();
	}

	public NorentesException(String message) {
		super(message);
	}

	public NorentesException(String message, Throwable cause) {
		super(message, cause);
	}

	public NorentesException(Throwable cause) {
		super(cause);
	}
}
