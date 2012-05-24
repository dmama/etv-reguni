package ch.vd.uniregctb.common;

public class ObjectNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -3468540885846141341L;

	public ObjectNotFoundException() {

	}

	public ObjectNotFoundException(String message) {
		super(message);
	}

	public ObjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
