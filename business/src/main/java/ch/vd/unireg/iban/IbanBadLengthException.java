package ch.vd.uniregctb.iban;

/**
 * Exception lancée lorsque la longueur d'un code IBAN est incorrecte.
 * @author Ludovic Bertin (OOSphere)
 *
 */
public class IbanBadLengthException extends IbanValidationException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1922076219274533456L;

	/**
	 * Constructeur par défaut.
	 */
	public IbanBadLengthException() {
		super("La longueur du code IBAN est incorrecte");
	}
}
