package ch.vd.uniregctb.role;

public class CalculRoleException extends RuntimeException {

	public CalculRoleException(String message) {
		super(message);
	}

	public CalculRoleException(String message, Throwable cause) {
		super(message, cause);
	}

	public CalculRoleException(Throwable cause) {
		super(cause);
	}

	public CalculRoleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
