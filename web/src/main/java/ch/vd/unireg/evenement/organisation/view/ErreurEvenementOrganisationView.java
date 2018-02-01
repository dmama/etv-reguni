package ch.vd.unireg.evenement.organisation.view;

import java.io.Serializable;

public final class ErreurEvenementOrganisationView implements Serializable {

	private static final long serialVersionUID = -3347886921558642160L;
	private final long errorId;
	private final String message;
	private final String callstack;

	public ErreurEvenementOrganisationView(long errorId, String message, String callstack) {
		this.errorId = errorId;
		this.message = message;
		this.callstack = callstack;
	}

	public long getErrorId() {
		return errorId;
	}

	public String getMessage() {
		return message;
	}

	@SuppressWarnings("unused")
	public String getCallstack() {
		return callstack;
	}
}
