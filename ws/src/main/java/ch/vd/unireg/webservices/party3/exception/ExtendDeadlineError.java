package ch.vd.uniregctb.webservices.party3.exception;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.party3.ExtendDeadlineCode;

public class ExtendDeadlineError extends Exception {

	private static final long serialVersionUID = 1L;

	private final ExtendDeadlineCode code;

	public ExtendDeadlineError(ExtendDeadlineCode code, String message) {
		super(message);
		Assert.isTrue(code.name().startsWith("ERROR"));
		this.code = code;
	}

	public ExtendDeadlineCode getCode() {
		return code;
	}
}
