package ch.vd.unireg.evenement.ide;

/**
 * @author Raphaël Marmier, 2016-10-06, <raphael.marmier@vd.ch>
 */
public class ReponseIDEProcessorException extends Exception {

	private static final long serialVersionUID = 1875196690105524717L;

	public ReponseIDEProcessorException() {
		super();
	}

	/**
	 * @param message message décrivant le problème.
	 */
	public ReponseIDEProcessorException(String message) {
		super(message);
	}

	/**
	 * @param message message décrivant le problème
	 * @param cause exception d'origine
	 */
	public ReponseIDEProcessorException(String message, Throwable cause) {
		super(message, cause);
	}
}
