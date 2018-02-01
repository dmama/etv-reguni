package ch.vd.unireg.evenement.ide;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public class AnnonceIDEException extends Exception {

	private static final long serialVersionUID = 785437083758472381L;

	public AnnonceIDEException() {
		super();
	}

	/**
	 * @param message message décrivant le problème.
	 */
	public AnnonceIDEException(String message) {
		super(message);
	}

	/**
	 * @param message message décrivant le problème
	 * @param cause exception d'origine
	 */
	public AnnonceIDEException(String message, Throwable cause) {
		super(message, cause);
	}
}
