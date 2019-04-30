package ch.vd.unireg.activation;

public class ActivationServiceException extends RuntimeException {

	public ActivationServiceException(String message) {
		super(message);
	}

	public ActivationServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
