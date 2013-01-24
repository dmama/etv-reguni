package ch.vd.uniregctb.metier.modeimposition;

import org.apache.commons.lang.StringUtils;

public class ModeImpositionResolverException extends Exception {

	private static final long serialVersionUID = -7241663335622467756L;

	public ModeImpositionResolverException(String message) {
		super(message);
	}

	public ModeImpositionResolverException(String message, Throwable t) {
		super(message, t);
	}

	@Override
	public String getMessage() {
		final String messageBase = super.getMessage();
		final Throwable cause = getCause();
		final String message;
		if (cause != null && !StringUtils.isBlank(cause.getMessage())) {
			message = String.format("%s (%s)", messageBase, cause.getMessage());
		}
		else {
			message = messageBase;
		}
		return message;
	}
}
