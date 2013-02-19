package ch.vd.uniregctb.evenement.identification.contribuable;

public class IdentificationNCSException extends Exception {

	private static final long serialVersionUID = -1L;

	public IdentificationNCSException(Throwable throwable) {
		super(throwable);
	}

	public IdentificationNCSException(String message) {
		super(message);
	}

	public IdentificationNCSException(String message, Throwable cause) {
		super(message, cause);
	}
}