package ch.vd.uniregctb.iban;

/**
 * Exception lancée lors de la validation d'un code IBAN
 * si ce dernier est incorrect. 
 * @author Ludovic Bertin (OOSphere)
 */
public class IbanValidationException extends Exception {

	/**
	 * Serial Version UID 
	 */
	private static final long serialVersionUID = 6593811050546878237L;

	/**
	 * {@inheritDoc}
	 */
	public IbanValidationException() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public IbanValidationException(String arg0) {
		super(arg0);
	}

	/**
	 * {@inheritDoc}
	 */
	public IbanValidationException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * {@inheritDoc}
	 */
	public IbanValidationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
