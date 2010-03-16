package ch.vd.uniregctb.migreg;

/**
 * Erreur lors du traitement de la migration.
 *
 * @author Gilles Dubey
 */
public class MigRegException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 6499175699485679829L;

	/**
	 * @param message
	 * @param t
	 */
	public MigRegException(final String message, final Throwable t) {
		super(message, t);
	}

	/**
	 *
	 * @param message
	 */
	public MigRegException(final String message) {
		super(message);
	}

}
