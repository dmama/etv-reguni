package ch.vd.uniregctb.common;

/**
 * Exception lancée par le {@link TicketService} dans le cas où un ticket ne peut pas être obtenu dans le temps imparti
 */
public class TicketTimeoutException extends Exception {
	public TicketTimeoutException() {
	}

	public TicketTimeoutException(String message) {
		super(message);
	}

	public TicketTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public TicketTimeoutException(Throwable cause) {
		super(cause);
	}

	public TicketTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
