package ch.vd.uniregctb.common;

/**
 * Exception dénotant une erreur de logique dans l'exécution d'un programme.
 */
public class ProgrammingException extends RuntimeException {

	private static final long serialVersionUID = 4167256304158899212L;

	public ProgrammingException() {

	}

	public ProgrammingException(String message) {
		super(message);
	}

	public ProgrammingException(String message, Throwable cause) {
		super(message, cause);
	}
}
