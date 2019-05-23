package ch.vd.unireg.interfaces.securite;

public class SecuriteConnectorException extends RuntimeException {

	private static final long serialVersionUID = -8038591562887325789L;

	public SecuriteConnectorException(String message) {
		super(message);
	}

	public SecuriteConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

	public SecuriteConnectorException(Throwable cause) {
		super(cause);
	}
}
