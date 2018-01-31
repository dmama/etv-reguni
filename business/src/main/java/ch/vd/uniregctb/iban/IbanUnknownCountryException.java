package ch.vd.uniregctb.iban;

/**
 * Exception lancée lorsque la longueur d'un code IBAN est incorrecte.
 * @author Ludovic Bertin (OOSphere)
 *
 */
public class IbanUnknownCountryException extends IbanValidationException {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6763437300055160046L;

	/**
	 * Constructeur par défaut.
	 */
	public IbanUnknownCountryException() {
		super("Le code Pays est inconnu");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IbanUnknownCountryException(String msg) {
		super("Le code Pays est inconnu : " + msg);
	}

}
