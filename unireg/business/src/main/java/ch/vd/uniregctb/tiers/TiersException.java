package ch.vd.uniregctb.tiers;

/**
 * Erreur lors du traitement d'un tiers.
 * 
 * @author Ludovic Bertin
 */
public class TiersException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4809515869214276599L;

	/**
	 * @param message
	 * @param t
	 */
	public TiersException(String message, Throwable t) {
		super(message, t);
	}

	/**
	 * 
	 * @param message
	 */
	public TiersException(String message) {
		super(message);
	}

}
