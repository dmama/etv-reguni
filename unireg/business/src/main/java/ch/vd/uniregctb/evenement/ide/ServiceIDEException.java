package ch.vd.uniregctb.evenement.ide;

/**
 * @author Raphaël Marmier, 2016-09-06, <raphael.marmier@vd.ch>
 */
public class ServiceIDEException extends Exception {

	private static final long serialVersionUID = -4827968681304634034L;

	public ServiceIDEException() {
		super();
	}

	/**
	 * @param message message décrivant le problème.
	 */
	public ServiceIDEException(String message) {
		super(message);
	}

	/**
	 * @param message message décrivant le problème
	 * @param cause exception d'origine
	 */
	public ServiceIDEException(String message, Throwable cause) {
		super(message, cause);
	}
}
