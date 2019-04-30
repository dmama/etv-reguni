package ch.vd.unireg.tiers;

/**
 * Erreur lors du traitement d'un tiers.
 * 
 * @author Ludovic Bertin
 */
public class TiersException extends RuntimeException {

	private static final long serialVersionUID = -4809515869214276599L;

	public TiersException(String message, Throwable t) {
		super(message, t);
	}

	public TiersException(String message) {
		super(message);
	}
}
