package ch.vd.uniregctb.evenement.addi;

/**
 * Exception métier liée à un événement ADDI.
 */
public class EvenementAddiException extends Exception {

	private static final long serialVersionUID = -1L;

	public EvenementAddiException(Throwable throwable) {
		super(throwable);
	}

	public EvenementAddiException(String message) {
		super(message);
	}

	public EvenementAddiException(String message, Throwable cause) {
		super(message, cause);
	}
}