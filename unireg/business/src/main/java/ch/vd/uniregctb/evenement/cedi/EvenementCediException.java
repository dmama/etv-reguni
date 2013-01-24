package ch.vd.uniregctb.evenement.cedi;

/**
 * Exception métier liée à un événement CEDI.
 */
public class EvenementCediException extends Exception {

	private static final long serialVersionUID = -1L;

	public EvenementCediException(Throwable throwable) {
		super(throwable);
	}

	public EvenementCediException(String message) {
		super(message);
	}

	public EvenementCediException(String message, Throwable cause) {
		super(message, cause);
	}
}