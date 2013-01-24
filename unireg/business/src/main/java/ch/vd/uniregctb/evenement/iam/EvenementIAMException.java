package ch.vd.uniregctb.evenement.iam;

/**
 * Exception métier liée à un événement IAM.
 */
public class EvenementIAMException extends Exception {

	private static final long serialVersionUID = -1L;

	public EvenementIAMException(Throwable throwable) {
		super(throwable);
	}

	public EvenementIAMException(String message) {
		super(message);
	}

	public EvenementIAMException(String message, Throwable cause) {
		super(message, cause);
	}
}