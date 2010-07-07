package ch.vd.uniregctb.iban;

/**
 * Exception lancée lorsque le numéro de clearing bancaire contenu dans le code IBAN est incorrect.
 * @author Ludovic Bertin (OOSphere)
 *
 */
public class IbanBadClearingNumberException extends IbanValidationException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6359236333231722554L;

	/**
	 * Constructeur par défaut.
	 */
	public IbanBadClearingNumberException() {
		super("Le numéro de clearing bancaire n'est pas plausible");
	}

	/**
	 * {@inheritDoc}
	 */
	public IbanBadClearingNumberException(String msg) {
		super("Le numéro de clearing bancaire n'est pas plausible : " + msg);
	}

}
