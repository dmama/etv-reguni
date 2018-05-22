package ch.vd.unireg.webservices.party3.exception;

import ch.vd.unireg.webservices.party3.ExtendDeadlineCode;

public class ExtendDeadlineError extends Exception {

	private static final long serialVersionUID = 1L;

	private final ExtendDeadlineCode code;

	public ExtendDeadlineError(ExtendDeadlineCode code, String message) {
		super(message);
		if (!code.name().startsWith("ERROR")) {
			throw new IllegalArgumentException();
		}
		this.code = code;
	}

	public ExtendDeadlineCode getCode() {
		return code;
	}
}
