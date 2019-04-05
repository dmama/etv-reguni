package ch.vd.unireg.metier.bouclement;

/**
 * Exception levée dans le service de bouclement.
 */
public class BouclementException extends RuntimeException {
	public BouclementException(String message) {
		super(message);
	}
}
