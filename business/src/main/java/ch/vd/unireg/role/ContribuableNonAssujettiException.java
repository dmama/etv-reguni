package ch.vd.unireg.role;

public class ContribuableNonAssujettiException extends CalculRoleException {

	public ContribuableNonAssujettiException(String message) {
		super(message);
	}

	public ContribuableNonAssujettiException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContribuableNonAssujettiException(Throwable cause) {
		super(cause);
	}

	public ContribuableNonAssujettiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
