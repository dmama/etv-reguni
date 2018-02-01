package ch.vd.uniregctb.iban;

/**
 * Exception lancée lorsque le format d'un code IBAN est incorrect.
 * @author Ludovic Bertin (OOSphere)
 *
 */
public class IbanBadFormatException extends IbanValidationException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4167755345604006444L;

	/**
	 * Constructeur par défaut.
	 */
	public IbanBadFormatException() {
		super("Le format est incorrect");
	}

	/**
	 * {@inheritDoc}
	 */
	public IbanBadFormatException(String msg) {
		super("Le format est incorrect : " + msg);
	}

}
