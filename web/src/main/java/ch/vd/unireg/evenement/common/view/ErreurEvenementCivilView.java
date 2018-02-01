package ch.vd.uniregctb.evenement.common.view;

import java.io.Serializable;

public final class ErreurEvenementCivilView implements Serializable {

	private static final long serialVersionUID = -4388468921640932612L;
	private final String message;
	private final String callstack;

	public ErreurEvenementCivilView(String message, String callstack) {
		this.message = message;
		this.callstack = callstack;
	}

	public String getMessage() {
		return message;
	}

	@SuppressWarnings("unused")
	public String getCallstack() {
		return callstack;
	}
}
