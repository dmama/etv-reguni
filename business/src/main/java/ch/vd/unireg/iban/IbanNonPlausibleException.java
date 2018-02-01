package ch.vd.uniregctb.iban;

/**
 * Exception lancée lorsque le code IBAN n'est pas plausible.
 * @author Ludovic Bertin (OOSphere)
 *
 */
public class IbanNonPlausibleException extends IbanValidationException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6359236333231722554L;

	/**
	 * Constructeur par défaut.
	 */
	public IbanNonPlausibleException() {
		super("Le code IBAN n'est pas plausible");
	}

	/**
	 * {@inheritDoc}
	 */
	public IbanNonPlausibleException(String msg) {
		super("Le code IBAN n'est pas plausible : " + msg);
	}

}
