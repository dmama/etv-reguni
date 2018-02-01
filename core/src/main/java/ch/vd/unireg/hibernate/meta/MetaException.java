package ch.vd.unireg.hibernate.meta;

public class MetaException extends Exception {

	public MetaException() {
	}

	public MetaException(String message) {
		super(message);
	}

	public MetaException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetaException(Throwable cause) {
		super(cause);
	}
}
