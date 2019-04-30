package ch.vd.unireg.webservices.common;

public class AccessDeniedException extends RuntimeException {

	public AccessDeniedException(String message) {
		super(message);
	}
}
