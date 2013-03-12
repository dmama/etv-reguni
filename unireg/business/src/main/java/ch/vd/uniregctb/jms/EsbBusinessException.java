package ch.vd.uniregctb.jms;

import ch.vd.technical.esb.ErrorType;

/**
 * Exception lancée par un {@link EsbMessageHandler} dans le cas où l'erreur est une erreur métier
 * qui ne doit pas partir en DLQ mais en queue d'erreur
 */
public class EsbBusinessException extends Exception {

	private final ErrorType errorType;
	private final String errorCode;

	public EsbBusinessException(String description, Exception cause, ErrorType errorType, String errorCode) {
		super(description, cause);
		this.errorType = errorType;
		this.errorCode = errorCode;
	}

	@Override
	public Exception getCause() {
		return (Exception) super.getCause();
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
